package pe.enrutatuagua.app.ui.vecino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.repositorio.TrackingRepository
import javax.inject.Inject

data class MapaVivoUiState(
    val cargando: Boolean = true,
    val posicion: PosicionCisterna? = null,
    val puntos: List<PuntoAbastecimiento> = emptyList(),
    val minutosSinSenal: Int = 0,
)

@HiltViewModel
class MapaVivoViewModel @Inject constructor(
    private val tracking: TrackingRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(MapaVivoUiState())
    val estado: StateFlow<MapaVivoUiState> = _estado

    init {
        viewModelScope.launch {
            combine(tracking.observarUltimaPosicion(), tracking.observarPuntosDeZona()) { pos, puntos ->
                val minutos = pos?.let { ((System.currentTimeMillis() - it.timestampMs) / 60_000).toInt() } ?: 0
                MapaVivoUiState(cargando = false, posicion = pos, puntos = puntos, minutosSinSenal = minutos)
            }.collect { _estado.value = it }
        }
    }
}
