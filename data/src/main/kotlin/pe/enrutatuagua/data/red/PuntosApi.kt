package pe.enrutatuagua.data.red

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class PuntoDto(
    val id: Long,
    val nombre: String,
    val lat: Double,
    val lng: Double,
    val capacidadLitros: Int,
    val estado: String? = null,
    val asociacionId: Long? = null,
)

interface PuntosApi {
    @GET("api/v1/puntos/{id}")
    suspend fun porId(@Path("id") id: Long): PuntoDto
}
