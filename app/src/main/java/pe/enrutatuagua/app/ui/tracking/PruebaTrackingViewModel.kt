package pe.enrutatuagua.app.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.repositorio.ContextoTrackingRepository
import pe.enrutatuagua.domain.repositorio.PosicionGps
import pe.enrutatuagua.domain.repositorio.PublicadorPosicionMqtt
import pe.enrutatuagua.domain.repositorio.TrackingRepository
import pe.enrutatuagua.app.ubicacion.AnclaTracking
import pe.enrutatuagua.app.ubicacion.ProveedorAnclaTracking
import pe.enrutatuagua.app.ubicacion.ResultadoAnclaTracking
import pe.enrutatuagua.app.ubicacion.rumboEntre
import kotlin.math.cos

data class PruebaTrackingUiState(
    val activa: Boolean = false,
    val enviados: Int = 0,
    val posicionRecibida: PosicionCisterna? = null,
    val rumbo: Float? = null,
    val ancla: AnclaTracking? = null,
    val ultimaRecepcionMs: Long? = null,
    val mensaje: String = "Lista para iniciar",
)

@HiltViewModel
class PruebaTrackingViewModel @Inject constructor(
    private val contextoTracking: ContextoTrackingRepository,
    private val publicador: PublicadorPosicionMqtt,
    tracking: TrackingRepository,
    private val proveedorAncla: ProveedorAnclaTracking,
) : ViewModel() {
    private val _estado = MutableStateFlow(PruebaTrackingUiState())
    val estado: StateFlow<PruebaTrackingUiState> = _estado
    private var simulacion: Job? = null

    private var inicioPruebaMs: Long? = null
    private val recorridoMetros = listOf(
        0.0 to 0.0, 8.0 to 7.0, 16.0 to 14.0, 24.0 to 21.0,
        32.0 to 28.0, 24.0 to 21.0, 16.0 to 14.0, 8.0 to 7.0,
    )

    init {
        viewModelScope.launch {
            tracking.observarUltimaPosicion()
                .catch { _estado.value = _estado.value.copy(mensaje = "Lectura falló: ${it.message}") }
                .collect { posicion ->
                    val inicio = inicioPruebaMs
                    if (posicion != null && inicio != null && posicion.timestampMs >= inicio) {
                        val anterior = _estado.value.posicionRecibida
                        val rumbo = if (anterior != null &&
                            (anterior.lat != posicion.lat || anterior.lng != posicion.lng)
                        ) rumboEntre(anterior.lat, anterior.lng, posicion.lat, posicion.lng)
                        else _estado.value.rumbo
                        _estado.value = _estado.value.copy(
                        posicionRecibida = posicion,
                        rumbo = rumbo,
                        ultimaRecepcionMs = System.currentTimeMillis(),
                        mensaje = "Recibida desde backend",
                    )
                    }
                }
        }
    }

    fun alternar() {
        if (simulacion != null) detener() else iniciar()
    }

    private fun iniciar() {
        simulacion = viewModelScope.launch {
            _estado.value = PruebaTrackingUiState(activa = true, mensaje = "Obteniendo tu ubicación…")
            val resultado = proveedorAncla.obtener()
            val ancla = when (resultado) {
                is ResultadoAnclaTracking.Disponible -> resultado.ancla
                ResultadoAnclaTracking.SinPermiso -> return@launch finalizar("Permiso de ubicación denegado")
                ResultadoAnclaTracking.UbicacionDesactivada -> return@launch finalizar("Activa la ubicación del teléfono y reintenta")
                ResultadoAnclaTracking.SinFixReciente -> return@launch finalizar("No se obtuvo una ubicación reciente. Acércate a una ventana y reintenta")
            }
            _estado.value = _estado.value.copy(ancla = ancla, mensaje = "Resolviendo ruta abierta…")
            var indice = 0
            while (true) {
                val contexto = contextoTracking.actual()
                if (contexto == null) {
                    return@launch finalizar("No hay una ruta ABIERTA para probar")
                }
                val desplazamiento = recorridoMetros[indice++ % recorridoMetros.size]
                val punto = desplazarMetros(ancla, desplazamiento.first, desplazamiento.second)
                val registradoEn = System.currentTimeMillis()
                if (inicioPruebaMs == null) inicioPruebaMs = registradoEn
                val ok = publicador.publicar(
                    contexto.aguateroId,
                    PosicionGps(
                        rutaId = contexto.rutaId,
                        lat = punto.lat,
                        lng = punto.lng,
                        registradaEnMillis = registradoEn,
                    ),
                )
                _estado.value = _estado.value.copy(
                    enviados = _estado.value.enviados + if (ok) 1 else 0,
                    mensaje = if (ok) "MQTT enviado; esperando backend…" else "No se pudo publicar en MQTT",
                )
                delay(2_000)
            }
        }
    }

    fun detener() {
        simulacion?.cancel()
        simulacion = null
        _estado.value = _estado.value.copy(activa = false, mensaje = "Prueba detenida")
        inicioPruebaMs = null
    }

    fun permisoRequerido() {
        _estado.value = _estado.value.copy(mensaje = "Concede ubicación precisa para iniciar cerca de ti")
    }

    private fun finalizar(mensaje: String) {
        simulacion = null
        inicioPruebaMs = null
        _estado.value = _estado.value.copy(activa = false, mensaje = mensaje)
    }

    override fun onCleared() {
        detener()
        super.onCleared()
    }
}

internal fun desplazarMetros(ancla: AnclaTracking, norteMetros: Double, esteMetros: Double): AnclaTracking {
    val lat = ancla.lat + norteMetros / 111_320.0
    val metrosPorGradoLng = 111_320.0 * cos(Math.toRadians(ancla.lat))
    return ancla.copy(lat = lat, lng = ancla.lng + esteMetros / metrosPorGradoLng)
}
