package pe.enrutatuagua.data.red

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

@Serializable data class FuenteCargaDto(val id:Long,val nombre:String,val lat:Double,val lng:Double,
    val radioPermitidoMetros:Int,val contactoNombre:String?=null,val contactoTelefono:String?=null,val activa:Boolean=true)
@Serializable data class SolicitudRecargaDto(val id:String,val jornadaId:Long,val rutaId:Long,val fuenteId:Long,
    val fuenteNombre:String,val solicitadaPor:Long,val aguateroNombre:String,val estado:Int,val litrosSolicitados:Int,
    val litrosReportados:Int?=null,val litrosConfirmados:Int?=null,val motivo:String,
    val contactoAutorizacion:String?=null,val contactoConfirmacion:String?=null,val notaOperador:String?=null,
    val distanciaFuenteMetros:Double?=null,val solicitadaEn:String,val autorizacionExpiraEn:String?=null)
@Serializable data class SolicitarRecargaDto(val mensajeId:String,val fuenteId:Long,val litros:Int,val motivo:String,
    val lat:Double?=null,val lng:Double?=null,val precisionMetros:Float?=null)
@Serializable data class ReportarCargaDto(val litros:Int,val lat:Double?=null,val lng:Double?=null,val precisionMetros:Float?=null)
@Serializable data class LlamadaOperadorDto(val contacto:String,val nota:String?=null)
@Serializable data class ConfirmarDespachoDto(val litros:Int,val contacto:String,val nota:String?=null)
@Serializable data class RechazarRecargaDto(val motivo:String)
@Serializable data class RecepcionPendienteDto(val rutaPuntoId:Long,val rutaId:Long,val puntoNombre:String,
    val litrosEsperados:Int,val litrosDeclarados:Int,val entregadaEn:String,val venceEn:String)
@Serializable data class ConfirmarRecepcionDto(val mensajeId:String,val litrosRecibidos:Int,val comentario:String?=null)
@Serializable data class RecepcionDto(val id:String,val litrosDeclarados:Int,val litrosRecibidos:Int,val estado:Int,
    val confirmadaPor:Long,val comentario:String?=null,val confirmadaEn:String)
@Serializable data class RecepcionDetalleDto(val id:String,val rutaPuntoId:Long,val rutaId:Long,val puntoNombre:String,
    val aguateroNombre:String,val litrosDeclarados:Int,val litrosRecibidos:Int,val estado:Int,
    val confirmadoPorNombre:String,val comentario:String?=null,val confirmadaEn:String)
@Serializable data class PaginaRecepcionesPendientesDto(val elementos:List<RecepcionPendienteDto>,val pagina:Int,
    val tamanio:Int,val totalElementos:Long,val totalPaginas:Int)
@Serializable data class PaginaRecepcionesDto(val elementos:List<RecepcionDetalleDto>,val pagina:Int,
    val tamanio:Int,val totalElementos:Long,val totalPaginas:Int)
@Serializable data class IncidenciaDto(val id:String,val jornadaId:Long?=null,val rutaId:Long?=null,val tipo:String,
    val severidad:Int,val estado:Int,val referencia:String,val detalle:String,val creadaEn:String,val resolucion:String?=null)
@Serializable data class ResolverIncidenciaDto(val estado:Int,val resolucion:String)

interface CustodiaAguaApi {
    @GET("api/v1/custodia/fuentes") suspend fun fuentes():List<FuenteCargaDto>
    @POST("api/v1/custodia/recargas") suspend fun solicitar(@Body request:SolicitarRecargaDto):SolicitudRecargaDto
    @GET("api/v1/custodia/recargas/activa") suspend fun activa():Response<SolicitudRecargaDto>
    @POST("api/v1/custodia/recargas/{id}/reportar") suspend fun reportar(@Path("id") id:String,@Body request:ReportarCargaDto):SolicitudRecargaDto
    @GET("api/v1/custodia/operador/recargas") suspend fun solicitudes(@Query("estado") estado:Int?=null):List<SolicitudRecargaDto>
    @POST("api/v1/custodia/operador/recargas/{id}/autorizar") suspend fun autorizar(@Path("id") id:String,@Body request:LlamadaOperadorDto):SolicitudRecargaDto
    @POST("api/v1/custodia/operador/recargas/{id}/confirmar") suspend fun confirmar(@Path("id") id:String,@Body request:ConfirmarDespachoDto):SolicitudRecargaDto
    @POST("api/v1/custodia/operador/recargas/{id}/rechazar") suspend fun rechazar(@Path("id") id:String,@Body request:RechazarRecargaDto):SolicitudRecargaDto
    @GET("api/v1/custodia/presidente/recepciones-pendientes") suspend fun recepciones():List<RecepcionPendienteDto>
    @GET("api/v1/custodia/presidente/recepciones/pendientes") suspend fun recepcionesPaginadas(
        @Query("page") pagina:Int,@Query("size") tamanio:Int):PaginaRecepcionesPendientesDto
    @GET("api/v1/custodia/presidente/recepciones/historial") suspend fun historialRecepciones(
        @Query("page") pagina:Int,@Query("size") tamanio:Int):PaginaRecepcionesDto
    @POST("api/v1/custodia/presidente/recepciones/{rutaPuntoId}/confirmar") suspend fun confirmarRecepcion(
        @Path("rutaPuntoId") rutaPuntoId:Long,@Body request:ConfirmarRecepcionDto):RecepcionDto
    @GET("api/v1/custodia/operador/incidencias") suspend fun incidencias(@Query("estado") estado:Int?=null):List<IncidenciaDto>
    @GET("api/v1/custodia/operador/recepciones") suspend fun recepcionesOperador(@Query("estado") estado:Int?=null,
        @Query("page") pagina:Int,@Query("size") tamanio:Int):PaginaRecepcionesDto
    @POST("api/v1/custodia/operador/incidencias/{id}/resolver") suspend fun resolver(
        @Path("id") id:String,@Body request:ResolverIncidenciaDto):IncidenciaDto
}
