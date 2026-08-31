package pe.enrutatuagua.data.red

import kotlinx.serialization.Serializable
import retrofit2.http.*

@Serializable data class AsociacionOperacionDto(val id:Long,val nombre:String)
@Serializable data class VehiculoOperacionDto(val id:Long,val placa:String,val capacidadLitros:Int,val disponible:Boolean=true,val motivoNoDisponible:String?=null)
@Serializable data class AguateroOperacionDto(val id:Long,val nombre:String,val disponible:Boolean=true,val motivoNoDisponible:String?=null)
@Serializable data class ResumenOperacionDto(val fecha:String,val asociaciones:List<AsociacionOperacionDto>,val vehiculos:List<VehiculoOperacionDto>,val aguateros:List<AguateroOperacionDto>)
@Serializable data class PedidoOperacionDto(val id:Long,val pedidoItemId:Long,val puntoId:Long,val puntoNombre:String,val litros:Int)
@Serializable data class SugerenciaRutaDto(val capacidadLitros:Int,val litrosSugeridos:Int,val sugeridos:List<PedidoOperacionDto>,val disponibles:List<PedidoOperacionDto>)
@Serializable data class PlanificarRutaRequestDto(val asociacionId:Long,val vehiculoId:Long,val aguateroId:Long,val fecha:String,val pedidoIds:List<Long>)
@Serializable data class RutaPlanificadaDto(val id:Long,val fecha:String,val asociacionId:Long,val vehiculoId:Long,val aguateroId:Long,val litros:Int,val estado:Int)
@Serializable data class RutaOperacionDto(val id:Long,val fecha:String,val estado:Int,val asociacion:AsociacionOperacionDto,val vehiculo:VehiculoOperacionDto,val aguatero:AguateroOperacionDto,val litros:Int,val paradas:Int)

interface OperacionApi {
    @GET("api/v1/operacion/resumen") suspend fun resumen(@Query("fecha") fecha:String):ResumenOperacionDto
    @GET("api/v1/operacion/sugerencia") suspend fun sugerencia(@Query("asociacionId") asociacionId:Long,@Query("vehiculoId") vehiculoId:Long):SugerenciaRutaDto
    @POST("api/v1/operacion/rutas") suspend fun planificar(@Body req:PlanificarRutaRequestDto):RutaPlanificadaDto
    @GET("api/v1/operacion/rutas") suspend fun listar(@Query("fecha") fecha:String?=null,@Query("estado") estado:Int?=null):List<RutaOperacionDto>
    @POST("api/v1/operacion/rutas/{id}/cancelar") suspend fun cancelar(@Path("id") id:Long):RutaPlanificadaDto
    @GET("api/v1/operacion/rutas/asignada") suspend fun asignada():RutaPlanificadaDto
    @POST("api/v1/operacion/rutas/{id}/iniciar") suspend fun iniciar(@Path("id") id:Long):RutaPlanificadaDto
}
