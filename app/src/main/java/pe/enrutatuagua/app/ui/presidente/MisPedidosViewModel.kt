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
import pe.enrutatuagua.domain.modelo.Pedido
import pe.enrutatuagua.domain.repositorio.PedidoRepository
import pe.enrutatuagua.domain.usecase.CancelarPedidoUseCase
import javax.inject.Inject

data class MisPedidosUiState(val cargando: Boolean = true, val pedidos: List<Pedido> = emptyList())

@HiltViewModel
class MisPedidosViewModel @Inject constructor(
    private val repo: PedidoRepository,
    private val cancelarPedido: CancelarPedidoUseCase,
) : ViewModel() {
    private val _estado = MutableStateFlow(MisPedidosUiState())
    val estado: StateFlow<MisPedidosUiState> = _estado
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            repo.observarPedidos()
                .retryWhen { causa, _ ->
                    _estado.value = _estado.value.copy(cargando = false)
                    _error.value = causa.mensajeParaUsuario()
                    delay(5_000)
                    true
                }
                .catch { causa ->
                    _estado.value = _estado.value.copy(cargando = false)
                    _error.value = causa.mensajeParaUsuario()
                }
                .collect {
                    _error.value = null
                    _estado.value = _estado.value.copy(cargando = false, pedidos = it)
                }
        }
    }

    fun cancelar(pedidoId: Long) {
        viewModelScope.launch {
            try { _error.value = null; cancelarPedido(pedidoId) }
            catch (e: Exception) { _error.value = e.mensajeParaUsuario() }
        }
    }

}
