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
import pe.enrutatuagua.domain.usecase.CrearPedidoUseCase
import javax.inject.Inject

@HiltViewModel
class NuevoPedidoViewModel @Inject constructor(
    repo: PedidoRepository,
    private val crearPedido: CrearPedidoUseCase,
) : ViewModel() {
    private val _puntos = MutableStateFlow<List<PuntoAbastecimiento>>(emptyList())
    // RN-03: un punto no aprobado no puede recibir pedidos.
    val puntos: StateFlow<List<PuntoAbastecimiento>> = _puntos

    private val _hecho = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    val hecho: StateFlow<Boolean> = _hecho
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
                    _puntos.value = lista.filter { it.estado == EstadoValidacionPunto.VALIDADO }
                }
        }
    }

    fun crear(puntoId: Long, litros: Int) {
        viewModelScope.launch {
            try { _error.value = null; crearPedido(puntoId, litros); _hecho.value = true }
            catch (e: Exception) { _error.value = e.mensajeParaUsuario() }
        }
    }
}
