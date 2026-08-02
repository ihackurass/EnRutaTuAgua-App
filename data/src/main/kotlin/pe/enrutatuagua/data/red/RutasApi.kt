package pe.enrutatuagua.data.red

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class ParadaDto(
    val id: Long,
    val puntoId: Long,
    val pedidoItemId: Long? = null,
    val orden: Int,
    val capacidadEsperadaLitros: Int,
    val estadoEntrega: String,
    val motivoOmision: String? = null,
    val comentarioOmision: String? = null,
)

@Serializable
data class RutaDto(
    val id: Long,
    val aguateroId: Long,
    val fecha: String,
    val estado: String,
    val vehiculoId: Long,
    val paradas: List<ParadaDto>,
    val cerradaEn: String? = null,
)

@Serializable
data class OmitirParadaRequestDto(val motivo: String, val comentario: String? = null)

@Serializable
data class RegistrarEntregaRequestDto(
    val id: String,
    val rutaPuntoId: Long,
    val tipo: String,
    val ajustaA: String? = null,
    val litros: Int,
    val gpsLat: Double? = null,
    val gpsLng: Double? = null,
    val registradaEn: String,
)

interface RutasApi {
    @GET("api/v1/rutas/abierta")
    suspend fun abierta(): RutaDto

    @POST("api/v1/rutas/abrir")
    suspend fun abrir(): RutaDto

    @POST("api/v1/rutas/{id}/cerrar")
    suspend fun cerrar(@Path("id") id: Long): RutaDto

    @POST("api/v1/rutas/{rutaId}/paradas/{rutaPuntoId}/omitir")
    suspend fun omitir(
        @Path("rutaId") rutaId: Long,
        @Path("rutaPuntoId") rutaPuntoId: Long,
        @Body solicitud: OmitirParadaRequestDto,
    ): ParadaDto

    // El body de la respuesta (mismo shape que la request, ver openapi.yaml
    // `Entrega: allOf: [RegistrarEntregaRequest]`) no se usa -- RutaRepositoryHttp
    // refresca la ruta completa después. Se declara igual (no Unit) para que
    // el converter de kotlinx.serialization tenga qué deserializar de verdad.
    @POST("api/v1/entregas")
    suspend fun registrarEntrega(@Body solicitud: RegistrarEntregaRequestDto): RegistrarEntregaRequestDto
}
