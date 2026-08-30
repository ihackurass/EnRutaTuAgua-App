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

data class PruebaTrackingUiState(
    val activa: Boolean = false,
    val enviados: Int = 0,
    val posicionRecibida: PosicionCisterna? = null,
    val mensaje: String = "Lista para iniciar",
)

@HiltViewModel
class PruebaTrackingViewModel @Inject constructor(
    private val contextoTracking: ContextoTrackingRepository,
    private val publicador: PublicadorPosicionMqtt,
    tracking: TrackingRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(PruebaTrackingUiState())
    val estado: StateFlow<PruebaTrackingUiState> = _estado
    private var simulacion: Job? = null

    private val recorrido = listOf(
        -12.1600 to -76.9400,
        -12.1597 to -76.9397,
        -12.1594 to -76.9394,
        -12.1591 to -76.9391,
        -12.1588 to -76.9388,
        -12.1591 to -76.9391,
        -12.1594 to -76.9394,
        -12.1597 to -76.9397,
    )

    init {
        viewModelScope.launch {
            tracking.observarUltimaPosicion()
                .catch { _estado.value = _estado.value.copy(mensaje = "Lectura falló: ${it.message}") }
                .collect { posicion ->
                    if (posicion != null) _estado.value = _estado.value.copy(
                        posicionRecibida = posicion,
                        mensaje = "Recibida desde backend",
                    )
                }
        }
    }

    fun alternar() {
        if (simulacion != null) detener() else iniciar()
    }

    private fun iniciar() {
        simulacion = viewModelScope.launch {
            _estado.value = _estado.value.copy(activa = true, mensaje = "Resolviendo ruta abierta…")
            var indice = 0
            while (true) {
                val contexto = contextoTracking.actual()
                if (contexto == null) {
                    _estado.value = _estado.value.copy(mensaje = "No hay una ruta ABIERTA para probar")
                    delay(3_000)
                    continue
                }
                val punto = recorrido[indice++ % recorrido.size]
                val ok = publicador.publicar(
                    contexto.aguateroId,
                    PosicionGps(
                        rutaId = contexto.rutaId,
                        lat = punto.first,
                        lng = punto.second,
                        registradaEnMillis = System.currentTimeMillis(),
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
    }

    override fun onCleared() {
        detener()
        super.onCleared()
    }
}
