package pe.enrutatuagua.app.ui.vecino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import pe.enrutatuagua.domain.modelo.EstadoValidacionPunto
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.modelo.EstadoTracking
import pe.enrutatuagua.domain.repositorio.TrackingRepository
import javax.inject.Inject

/** Intervalo del ticker de UI que llama a `recalcularMinutosSinSenal()` (ver MapaVivoScreen.kt). */
const val INTERVALO_TICK_RELOJ_MS = 30_000L

data class MapaVivoUiState(
    val cargando: Boolean = true,
    val asociacionNombre: String = "Tu asociación",
    val posicion: PosicionCisterna? = null,
    val puntos: List<PuntoAbastecimiento> = emptyList(),
    val minutosSinSenal: Int = 0,
    val estadoTracking: EstadoTracking = EstadoTracking.SIN_POSICION,
)

/** Auditoría rol Vecino: mismo patrón de resiliencia ya corregido en RutaViewModel (rol Aguatero). */
@HiltViewModel
class MapaVivoViewModel @Inject constructor(
    private val tracking: TrackingRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(MapaVivoUiState())
    val estado: StateFlow<MapaVivoUiState> = _estado

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    // Seam de test para el reloj -- propiedad mutable en vez de parámetro
    // @Inject: Dagger no puede bindear un Function0<Long> anónimo.
    internal var ahora: () -> Long = { System.currentTimeMillis() }

    init {
        viewModelScope.launch {
            try {
                _estado.value = _estado.value.copy(asociacionNombre = tracking.asociacionActual())
            } catch (_: Exception) {
                // El mapa y los puntos siguen siendo útiles aunque este dato auxiliar falle.
            }
        }
        viewModelScope.launch {
            try {
                combine(
                    // Hallazgo real (auditoría rol Vecino): sin onStart{}, si
                    // un flow tarda más que el otro en emitir su primer
                    // valor (posible una vez TrackingRepositoryHttp haga
                    // polling real), combine() no produce nada y
                    // cargando=true queda pegado para siempre.
                    tracking.observarUltimaPosicion().onStart { emit(null) },
                    tracking.observarPuntosDeZona().onStart { emit(emptyList()) },
                    tracking.observarEstadoTracking().onStart { emit(EstadoTracking.SIN_POSICION) },
                ) { posicion, puntos, estadoTracking ->
                    val minutos = posicion?.let { ((ahora() - it.timestampMs) / 60_000).toInt() } ?: 0
                    // Hallazgo real: puntos RECHAZADO/SUSPENDIDO se veían
                    // igual que PROPUESTO en mapa y lista -- sólo VALIDADO
                    // es ruteable/relevante para un vecino buscando agua.
                    val validados = puntos.filter { it.estado == EstadoValidacionPunto.VALIDADO }
                    MapaVivoUiState(cargando = false, asociacionNombre = _estado.value.asociacionNombre, posicion = posicion, puntos = validados, minutosSinSenal = minutos, estadoTracking = estadoTracking)
                }
                    .retry { e ->
                        _mensaje.value = e.message ?: "No se pudo cargar tu zona, reintentando…"
                        delay(5_000L)
                        true
                    }
                    .collect { _estado.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Hallazgo real (auditoría rol Vecino): sin este catch, una
                // excepción no atrapada acá crasheaba toda la app -- mismo
                // patrón de bug que ya se corrigió para RutaViewModel.
                _estado.value = _estado.value.copy(cargando = false)
                _mensaje.value = e.message ?: "No se pudo cargar tu zona, revisá tu conexión"
            }
        }
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }

    /**
     * Hallazgo real (auditoría rol Vecino): minutosSinSenal sólo se
     * recalculaba cuando el combine() de arriba reemitía -- con un backend
     * real que no reemite posición nueva todo el tiempo, el badge "hace N
     * min" quedaba congelado. Llamado desde un ticker en MapaVivoScreen.kt
     * (LaunchedEffect), NO desde un flow perpetuo dentro del ViewModel: un
     * `while(true){delay()}` interno rompía tanto `waitForIdle()` de Compose
     * como la finalización propia de `runTest` en los tests unitarios (el
     * scheduler compartido vía Dispatchers.setMain nunca terminaba de
     * "drenar" un timer que se reprograma a sí mismo para siempre).
     */
    fun recalcularMinutosSinSenal() {
        val posicion = _estado.value.posicion ?: return
        _estado.value = _estado.value.copy(minutosSinSenal = ((ahora() - posicion.timestampMs) / 60_000).toInt())
    }
}
