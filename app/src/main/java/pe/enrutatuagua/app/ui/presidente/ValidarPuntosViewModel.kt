package pe.enrutatuagua.app.ui.presidente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.EstadoValidacionPunto
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.repositorio.PedidoRepository
import pe.enrutatuagua.domain.usecase.ValidarPuntoUseCase
import javax.inject.Inject

@HiltViewModel
class ValidarPuntosViewModel @Inject constructor(
    repo: PedidoRepository,
    private val validarPunto: ValidarPuntoUseCase,
) : ViewModel() {
    private val _puntos = MutableStateFlow<List<PuntoAbastecimiento>>(emptyList())
    val puntos: StateFlow<List<PuntoAbastecimiento>> = _puntos

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            repo.observarPuntosDeZona()
                .retryWhen { causa, _ ->
                    _error.value = causa.mensajeParaUsuario()
                    delay(5_000)
                    true
                }
                .catch { _error.value = it.mensajeParaUsuario() }
                .collect { lista ->
                    _error.value = null
                    _puntos.value = lista.filter { it.estado == EstadoValidacionPunto.PROPUESTO }
                }
        }
    }

    // No navega: la lista se actualiza sola porque el Flow reacciona.
    fun validar(puntoId: Long) {
        viewModelScope.launch {
            try { _error.value = null; validarPunto(puntoId) }
            catch (e: Exception) { _error.value = e.mensajeParaUsuario() }
        }
    }
}
