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

interface PedidosApi {
    @GET("api/v1/pedidos") suspend fun listar(): List<PedidoDto>
    @POST("api/v1/pedidos") suspend fun crear(@Body solicitud: CrearPedidoRequestDto): PedidoDto
    @POST("api/v1/pedidos/{id}/cancelar") suspend fun cancelar(@Path("id") id: Long): PedidoDto
}
