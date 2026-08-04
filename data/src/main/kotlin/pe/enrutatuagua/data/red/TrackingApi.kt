package pe.enrutatuagua.data.red

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class PosicionVivaDto(val rutaId: Long, val lat: Double, val lng: Double, val registradoEn: String)

interface TrackingApi {
    /** 404 (HttpException) si no hay fix reciente para esa ruta -- estado válido, no error. */
    @GET("api/v1/tracking/rutas/{rutaId}/posicion")
    suspend fun posicionViva(@Path("rutaId") rutaId: Long): PosicionVivaDto
}
