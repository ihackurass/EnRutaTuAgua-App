package pe.enrutatuagua.data.red

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.PATCH
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

@Serializable
data class CambiarEstadoPuntoRequestDto(val estado: String, val comentario: String? = null)

interface PuntosApi {
    @GET("api/v1/puntos/{id}")
    suspend fun porId(@Path("id") id: Long): PuntoDto

    /** Auditoría rol Vecino: todos los puntos de la asociación del actor, sin filtrar por estado. */
    @GET("api/v1/puntos/zona")
    suspend fun zona(): List<PuntoDto>

    @PATCH("api/v1/puntos/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: Long,
        @Body solicitud: CambiarEstadoPuntoRequestDto,
    ): PuntoDto
}
