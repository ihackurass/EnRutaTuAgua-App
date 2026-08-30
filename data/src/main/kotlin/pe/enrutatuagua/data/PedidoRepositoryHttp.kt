package pe.enrutatuagua.data

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
import pe.enrutatuagua.domain.repositorio.PedidoRepository

private const val POLLING_PEDIDOS_MS = 10_000L

@Singleton
class PedidoRepositoryHttp @Inject constructor(
    private val pedidosApi: PedidosApi,
    private val puntosApi: PuntosApi,
) : PedidoRepository {
    override fun observarPedidos(): Flow<List<Pedido>> = flow {
        while (true) { emit(pedidosApi.listar().map(PedidoDto::aDominio)); delay(POLLING_PEDIDOS_MS) }
    }

    override fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>> = flow {
        while (true) { emit(puntosApi.zona().map(PuntoDto::aDominio)); delay(POLLING_PEDIDOS_MS) }
    }

    override suspend fun crearPedido(puntoId: Long, litros: Int): Pedido =
        pedidosApi.crear(CrearPedidoRequestDto(puntoId, litros)).aDominio()

    override suspend fun cancelarPedido(pedidoId: Long) { pedidosApi.cancelar(pedidoId) }

    override suspend fun validarPunto(puntoId: Long) {
        puntosApi.cambiarEstado(puntoId, CambiarEstadoPuntoRequestDto(EstadoValidacionPunto.VALIDADO.name))
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
