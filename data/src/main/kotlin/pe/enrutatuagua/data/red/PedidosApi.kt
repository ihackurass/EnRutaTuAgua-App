package pe.enrutatuagua.data.red

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable data class CrearPedidoRequestDto(val puntoId: Long, val litros: Int)
@Serializable data class PedidoItemDto(val puntoId: Long, val litros: Int)
@Serializable data class PedidoDto(
    val id: Long,
    val items: List<PedidoItemDto>,
    val estado: String,
    val creadoEn: String,
)
@Serializable data class LimitePedidoDto(val capacidadMaximaCisternaLitros: Int, val maxLitrosPedido: Int, val tienePedidoActivo: Boolean)

interface PedidosApi {
    @GET("api/v1/pedidos") suspend fun listar(): List<PedidoDto>
    @POST("api/v1/pedidos") suspend fun crear(@Body solicitud: CrearPedidoRequestDto): PedidoDto
    @POST("api/v1/pedidos/{id}/cancelar") suspend fun cancelar(@Path("id") id: Long): PedidoDto
    @GET("api/v1/pedidos/limites/{puntoId}") suspend fun limite(@Path("puntoId") puntoId: Long): LimitePedidoDto
}
