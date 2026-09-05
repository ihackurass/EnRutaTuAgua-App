package pe.enrutatuagua.app.ui.ruta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import pe.enrutatuagua.domain.repositorio.RutaRepository
import pe.enrutatuagua.domain.repositorio.SaldoJornada
import java.util.UUID
import pe.enrutatuagua.domain.usecase.ObtenerRutaAbiertaUseCase
import pe.enrutatuagua.domain.usecase.RutaConTanque
import pe.enrutatuagua.domain.modelo.EstadoSyncOperativo
import pe.enrutatuagua.domain.modelo.FuenteCarga
import pe.enrutatuagua.domain.modelo.SolicitudRecarga
import pe.enrutatuagua.domain.modelo.EstadoSolicitudRecarga
import pe.enrutatuagua.domain.repositorio.CustodiaAguaRepository
import pe.enrutatuagua.app.ubicacion.ProveedorAnclaTracking
import pe.enrutatuagua.app.ubicacion.ResultadoAnclaTracking
import javax.inject.Inject

data class RutaUiState(
    val cargando: Boolean = true,
    val rutaConTanque: RutaConTanque? = null,
    val procesandoAccion: Boolean = false,
    val jornada: SaldoJornada? = null,
    val sync: EstadoSyncOperativo = EstadoSyncOperativo(),
    val fuentes: List<FuenteCarga> = emptyList(),
    val solicitudRecarga: SolicitudRecarga? = null,
)

/** ANDROID-PLAN-02 (T5): "hoy salgo a repartir" / cerrar ruta -- sin validación propia, el backend es la única autoridad. */
@HiltViewModel
class RutaViewModel @Inject constructor(
    obtenerRutaAbierta: ObtenerRutaAbiertaUseCase,
    private val repo: RutaRepository,
    private val custodia: CustodiaAguaRepository,
    private val ubicacion: ProveedorAnclaTracking,
) : ViewModel() {
    private val _estado = MutableStateFlow(RutaUiState())
    val estado: StateFlow<RutaUiState> = _estado

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje
    private var mensajeCierrePendiente: String? = null

    init {
        viewModelScope.launch { repo.observarEstadoSync().collect { _estado.value=_estado.value.copy(sync=it) } }
        viewModelScope.launch {
            runCatching { custodia.fuentes() }.onSuccess { _estado.value=_estado.value.copy(fuentes=it) }
            refrescarCustodia()
        }
        viewModelScope.launch {
            try { _estado.value = _estado.value.copy(jornada = repo.consultarJornada()) }
            catch (e: Exception) { _mensaje.value = e.message }
        }
        viewModelScope.launch {
            repo.sembrarSiVacio()
            try {
                // Hallazgo real (auditoría rol Aguatero): sin retry, un
                // fallo en la carga inicial dejaba el collect muerto para
                // siempre -- había que reabrir la pantalla. Mismo patrón ya
                // usado en ObservadorRutaAbiertaGps/PrecacheRutaObservador
                // (reintenta cada 5s, infinito); seguro acá porque
                // RutaRepositoryHttp.observarRutaAbierta() sólo puede fallar
                // en la carga inicial -- una vez bootstrapeado no vuelve a
                // lanzar.
                obtenerRutaAbierta()
                    .retry { e ->
                        _mensaje.value = e.message ?: "No se pudo cargar la ruta, reintentando…"
                        delay(5_000L)
                        true
                    }
                    .collect { rc ->
                        val jornada = try { repo.consultarJornada() } catch (_: Exception) { _estado.value.jornada }
                        _estado.value = _estado.value.copy(cargando = false, rutaConTanque = rc, jornada = jornada)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Hallazgo real (T8): un SocketTimeoutException acá no es un
                // HttpException (falla antes de llegar a tener respuesta),
                // así que RutaRepositoryHttp no lo puede tratar como
                // SinRutaAbierta -- sin este catch, tumbaba toda la app en
                // vez de mostrar el error (viola "sin cola offline, error
                // visible" del plan). Red de seguridad para lo que quede
                // fuera del predicado de retry de arriba.
                _estado.value = _estado.value.copy(cargando = false, rutaConTanque = null)
                _mensaje.value = e.message ?: "No se pudo cargar la ruta, revisá tu conexión"
            }
        }
    }

    fun abrirRuta() = ejecutar {
        repo.abrirRuta()
        _estado.value = _estado.value.copy(jornada = repo.consultarJornada())
    }
    fun cerrarRuta(litrosFisicosFinales: Int) = ejecutar {
        val mensajeId = mensajeCierrePendiente ?: UUID.randomUUID().toString().also { mensajeCierrePendiente=it }
        repo.cerrarRuta(litrosFisicosFinales, mensajeId)
        mensajeCierrePendiente=null
        _estado.value = _estado.value.copy(jornada = repo.consultarJornada())
    }
    fun registrarCarga(litros: Int) = ejecutar {
        val saldo = repo.registrarCargaInicial(UUID.randomUUID().toString(), litros)
        _estado.value = _estado.value.copy(jornada = saldo)
    }
    fun solicitarRecarga(fuenteId:Long,litros:Int,motivo:String) = ejecutar {
        val gps=ubicacion.obtener().gps()
        val solicitud=custodia.solicitarRecarga(UUID.randomUUID().toString(),fuenteId,litros,motivo,
            gps?.lat,gps?.lng,gps?.precisionMetros)
        _estado.value=_estado.value.copy(solicitudRecarga=solicitud)
        _mensaje.value="Solicitud enviada al operador"
    }
    fun reportarCarga(litros:Int) = ejecutar {
        val solicitud=checkNotNull(_estado.value.solicitudRecarga)
        val gps=ubicacion.obtener().gps()
        _estado.value=_estado.value.copy(solicitudRecarga=custodia.reportarCarga(solicitud.id,litros,gps?.lat,gps?.lng,gps?.precisionMetros))
        _mensaje.value="Carga reportada; esperando la segunda llamada del operador"
    }
    fun mensajeMostrado() { _mensaje.value = null }
    suspend fun refrescarCustodia() {
        val anterior=_estado.value.solicitudRecarga
        runCatching { custodia.solicitudActiva() }.onSuccess { actual ->
            if (anterior?.estado==EstadoSolicitudRecarga.CARGA_REPORTADA && actual==null) {
                _mensaje.value="Recarga confirmada por el operador"
                runCatching { repo.consultarJornada() }.onSuccess { saldo ->
                    _estado.value=_estado.value.copy(jornada=saldo)
                }
            }
            _estado.value=_estado.value.copy(solicitudRecarga=actual)
        }
    }
    fun reintentarSync() = ejecutar { repo.reintentarSync() }
    fun reconocerRechazadas() = ejecutar { repo.reconocerRechazadas() }

    private fun ejecutar(accion: suspend () -> Unit) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(procesandoAccion = true)
            try {
                accion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "Ocurrió un error, probá de nuevo"
            } finally {
                _estado.value = _estado.value.copy(procesandoAccion = false)
            }
        }
    }

    private fun ResultadoAnclaTracking.gps() = (this as? ResultadoAnclaTracking.Disponible)?.ancla
}
