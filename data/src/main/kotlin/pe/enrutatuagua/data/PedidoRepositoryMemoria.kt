package pe.enrutatuagua.data

import kotlinx.coroutines.flow.Flow
import pe.enrutatuagua.domain.modelo.Pedido
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.repositorio.PedidoRepository
import pe.enrutatuagua.domain.usecase.PedidoRepoFake
import javax.inject.Inject
import javax.inject.Singleton

// §0 del plan 06: pedidos y puntos de asociación no se cachean en el MVP
// (MODELO-DATOS-ER.md §6) — no hay nada que Room deba persistir aquí. Se
// reutiliza el fake también como implementación real-de-mentira en
// producción, a propósito: el día que exista backend, solo se reemplaza
// este binding en DataModule.kt; el dominio y la UI no cambian.
@Singleton
class PedidoRepositoryMemoria @Inject constructor() : PedidoRepository {
    private val fake = PedidoRepoFake()

    override fun observarPedidos(): Flow<List<Pedido>> = fake.observarPedidos()
    override fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>> = fake.observarPuntosDeZona()
    override suspend fun crearPedido(puntoId: Long, litros: Int): Pedido = fake.crearPedido(puntoId, litros)
    override suspend fun cancelarPedido(pedidoId: Long) = fake.cancelarPedido(pedidoId)
    override suspend fun validarPunto(puntoId: Long) = fake.validarPunto(puntoId)
    override suspend fun consultarLimite(puntoId: Long) = fake.consultarLimite(puntoId)
}
