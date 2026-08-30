package pe.enrutatuagua.data

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import pe.enrutatuagua.data.red.CambiarEstadoPuntoRequestDto
import pe.enrutatuagua.data.red.CrearPedidoRequestDto
import pe.enrutatuagua.data.red.PedidoDto
import pe.enrutatuagua.data.red.PedidosApi
import pe.enrutatuagua.data.red.PuntoDto
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.domain.modelo.EstadoPedido
import pe.enrutatuagua.domain.modelo.EstadoValidacionPunto
import pe.enrutatuagua.domain.modelo.Pedido
import pe.enrutatuagua.domain.modelo.PedidoItem
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.modelo.LimitePedido
import pe.enrutatuagua.domain.repositorio.PedidoRepository

private const val POLLING_PEDIDOS_MS = 10_000L

@Singleton
class PedidoRepositoryHttp @Inject constructor(
    private val pedidosApi: PedidosApi,
    private val puntosApi: PuntosApi,
) : PedidoRepository {
    private val refrescarPedidos = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val refrescarPuntos = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun observarPedidos(): Flow<List<Pedido>> =
        merge(reloj(), refrescarPedidos.onStart { emit(Unit) })
            .map { pedidosApi.listar().map(PedidoDto::aDominio).sortedByDescending(Pedido::creadoEn) }

    override fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>> =
        merge(reloj(), refrescarPuntos.onStart { emit(Unit) })
            .map { puntosApi.zona().map(PuntoDto::aDominio) }

    override suspend fun crearPedido(puntoId: Long, litros: Int): Pedido {
        val creado = pedidosApi.crear(CrearPedidoRequestDto(puntoId, litros)).aDominio()
        refrescarPedidos.tryEmit(Unit)
        return creado
    }

    override suspend fun cancelarPedido(pedidoId: Long) {
        pedidosApi.cancelar(pedidoId)
        refrescarPedidos.tryEmit(Unit)
    }

    override suspend fun validarPunto(puntoId: Long) {
        puntosApi.cambiarEstado(puntoId, CambiarEstadoPuntoRequestDto(EstadoValidacionPunto.VALIDADO.name))
        refrescarPuntos.tryEmit(Unit)
    }
    override suspend fun consultarLimite(puntoId: Long): LimitePedido = pedidosApi.limite(puntoId).let {
        LimitePedido(it.capacidadMaximaCisternaLitros, it.maxLitrosPedido, it.tienePedidoActivo)
    }

    private fun reloj(): Flow<Unit> = flow {
        while (true) { delay(POLLING_PEDIDOS_MS); emit(Unit) }
    }
}

private fun PedidoDto.aDominio() = Pedido(
    id = id,
    items = items.map { PedidoItem(it.puntoId, it.litros) },
    estado = EstadoPedido.valueOf(estado),
    creadoEn = Instant.parse(creadoEn).toEpochMilli(),
)

private fun PuntoDto.aDominio() = PuntoAbastecimiento(
    id, nombre, lat, lng, capacidadLitros,
    estado?.let(EstadoValidacionPunto::valueOf) ?: EstadoValidacionPunto.PROPUESTO,
)
