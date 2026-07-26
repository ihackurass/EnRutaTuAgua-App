package pe.enrutatuagua.domain.usecase

import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.Pedido
import pe.enrutatuagua.domain.repositorio.PedidoRepository
import javax.inject.Inject

class CrearPedidoUseCase @Inject constructor(private val repo: PedidoRepository) {
    suspend operator fun invoke(puntoId: Long, litros: Int): Pedido {
        if (litros <= 0) throw ErrorDominio.PedidoInvalido()
        return repo.crearPedido(puntoId, litros)
    }
}
