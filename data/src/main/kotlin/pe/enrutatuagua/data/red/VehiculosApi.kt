package pe.enrutatuagua.data.red

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class VehiculoDto(val id: Long, val placa: String, val capacidadTanqueLitros: Int)

interface VehiculosApi {
    @GET("api/v1/vehiculos/{id}")
    suspend fun porId(@Path("id") id: Long): VehiculoDto
}
