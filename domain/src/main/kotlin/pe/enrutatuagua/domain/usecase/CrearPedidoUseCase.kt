package pe.enrutatuagua.domain.usecase

import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.Pedido
import pe.enrutatuagua.domain.modelo.PedidoItem
import pe.enrutatuagua.domain.modelo.CondicionesPedido
import pe.enrutatuagua.domain.modelo.PrioridadPedido
import pe.enrutatuagua.domain.repositorio.PedidoRepository
import javax.inject.Inject

class CrearPedidoUseCase @Inject constructor(private val repo: PedidoRepository) {
    suspend operator fun invoke(puntoId: Long, litros: Int): Pedido {
        if (litros <= 0) throw ErrorDominio.PedidoInvalido()
        return repo.crearPedido(puntoId, litros)
    }

    suspend operator fun invoke(items: List<PedidoItem>): Pedido {
        return invoke(items, CondicionesPedido())
    }

    suspend operator fun invoke(items: List<PedidoItem>, condiciones: CondicionesPedido): Pedido {
        if (items.isEmpty() || items.size > 20 || items.any { it.litros <= 0 } ||
            items.map { it.puntoId }.distinct().size != items.size) throw ErrorDominio.PedidoInvalido()
        if ((condiciones.ventanaDesde == null) != (condiciones.ventanaHasta == null) ||
            condiciones.ventanaDesde != null && condiciones.ventanaHasta!! <= condiciones.ventanaDesde ||
            condiciones.prioridad == PrioridadPedido.URGENTE && condiciones.motivoPrioridad.orEmpty().trim().length < 10
        ) throw ErrorDominio.PedidoInvalido()
        return repo.crearPedidoMultipunto(items, condiciones)
    }
}
