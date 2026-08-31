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
import javax.inject.Inject

data class RutaUiState(
    val cargando: Boolean = true,
    val rutaConTanque: RutaConTanque? = null,
    val procesandoAccion: Boolean = false,
    val jornada: SaldoJornada? = null,
)

/** ANDROID-PLAN-02 (T5): "hoy salgo a repartir" / cerrar ruta -- sin validación propia, el backend es la única autoridad. */
@HiltViewModel
class RutaViewModel @Inject constructor(
    obtenerRutaAbierta: ObtenerRutaAbiertaUseCase,
    private val repo: RutaRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(RutaUiState())
    val estado: StateFlow<RutaUiState> = _estado

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    init {
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
    fun cerrarRuta() = ejecutar {
        repo.cerrarRuta()
        _estado.value = _estado.value.copy(jornada = repo.consultarJornada())
    }
    fun registrarCarga(litros: Int) = ejecutar {
        val saldo = repo.registrarCargaInicial(UUID.randomUUID().toString(), litros)
        _estado.value = _estado.value.copy(jornada = saldo)
    }
    fun recargar(litros: Int) = ejecutar {
        val saldo = repo.registrarRecarga(UUID.randomUUID().toString(), litros)
        _estado.value = _estado.value.copy(jornada = saldo)
    }
    fun mensajeMostrado() { _mensaje.value = null }

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
}
