package pe.enrutatuagua.app.ui.ruta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pe.enrutatuagua.domain.repositorio.RutaRepository
import pe.enrutatuagua.domain.usecase.ObtenerRutaAbiertaUseCase
import pe.enrutatuagua.domain.usecase.RutaConTanque
import javax.inject.Inject

data class RutaUiState(val cargando: Boolean = true, val rutaConTanque: RutaConTanque? = null)

@HiltViewModel
class RutaViewModel @Inject constructor(
    obtenerRutaAbierta: ObtenerRutaAbiertaUseCase,
    private val repo: RutaRepository,        // solo para el seed del MVP fake
) : ViewModel() {
    private val _estado = MutableStateFlow(RutaUiState())
    val estado: StateFlow<RutaUiState> = _estado

    init {
        viewModelScope.launch {
            repo.sembrarSiVacio()
            obtenerRutaAbierta().collect { _estado.value = RutaUiState(cargando = false, rutaConTanque = it) }
        }
    }
}
