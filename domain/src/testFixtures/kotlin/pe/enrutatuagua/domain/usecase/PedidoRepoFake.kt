package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pe.enrutatuagua.domain.modelo.*
import pe.enrutatuagua.domain.repositorio.PedidoRepository

class PedidoRepoFake : PedidoRepository {
    val puntosFlow = MutableStateFlow(puntosDePrueba())
    val pedidosFlow = MutableStateFlow<List<Pedido>>(emptyList())
    private var siguienteId = 1L

    override fun observarPedidos(): Flow<List<Pedido>> = pedidosFlow
    override fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>> = puntosFlow

    override suspend fun crearPedido(puntoId: Long, litros: Int): Pedido {
        return crearPedidoMultipunto(listOf(PedidoItem(puntoId, litros)))
    }

    override suspend fun crearPedidoMultipunto(items: List<PedidoItem>): Pedido {
        val pedido = Pedido(siguienteId++, items, EstadoPedido.PENDIENTE, 0L)
        pedidosFlow.value = pedidosFlow.value + pedido
        return pedido
    }

    override suspend fun cancelarPedido(pedidoId: Long) {
        pedidosFlow.value = pedidosFlow.value.map {
            if (it.id == pedidoId) it.copy(estado = EstadoPedido.CANCELADO) else it
        }
    }

    override suspend fun validarPunto(puntoId: Long) {
        puntosFlow.value = puntosFlow.value.map {
            if (it.id == puntoId) it.copy(estado = EstadoValidacionPunto.VALIDADO) else it
        }
    }
    override suspend fun consultarLimite(puntoId: Long) = pe.enrutatuagua.domain.modelo.LimitePedido(
        12_000, puntosFlow.value.first { it.id == puntoId }.capacidadLitros,
        pedidosFlow.value.any { p -> p.estado !in setOf(EstadoPedido.CANCELADO, EstadoPedido.ATENDIDO) && p.items.any { it.puntoId == puntoId } },
    )

    /** Solo para tests: fuerza un estado sin pasar por las reglas del use case. */
    fun forzarEstado(pedidoId: Long, estado: EstadoPedido) {
        pedidosFlow.value = pedidosFlow.value.map { if (it.id == pedidoId) it.copy(estado = estado) else it }
    }

    companion object {
        fun puntosDePrueba() = listOf(
            PuntoAbastecimiento(1, "Comité 12 — Loma Alta", -12.1571, -76.9442, 4000, EstadoValidacionPunto.VALIDADO),
            PuntoAbastecimiento(2, "AAHH Buena Vista, sector B", -12.1613, -76.9488, 3000, EstadoValidacionPunto.PROPUESTO),
            PuntoAbastecimiento(3, "Comité 7 — José C. Mariátegui", -12.1652, -76.9521, 6000, EstadoValidacionPunto.VALIDADO),
        )
    }
}
