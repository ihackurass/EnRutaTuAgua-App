package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.flow.first
import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.EstadoPedido
import pe.enrutatuagua.domain.repositorio.PedidoRepository
import javax.inject.Inject

// I-10: solo cancelable desde PENDIENTE o RETENIDO — la regla vive aquí,
// no en el botón de la pantalla (mismo principio que RN-01 del aguatero).
class CancelarPedidoUseCase @Inject constructor(private val repo: PedidoRepository) {
    suspend operator fun invoke(pedidoId: Long) {
        val pedido = repo.observarPedidos().first().firstOrNull { it.id == pedidoId }
            ?: throw ErrorDominio.PedidoNoCancelable()
        if (pedido.estado != EstadoPedido.PENDIENTE && pedido.estado != EstadoPedido.RETENIDO) {
            throw ErrorDominio.PedidoNoCancelable()
        }
        repo.cancelarPedido(pedidoId)
    }
}
