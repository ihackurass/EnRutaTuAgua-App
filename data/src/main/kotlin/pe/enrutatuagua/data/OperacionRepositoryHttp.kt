package pe.enrutatuagua.data

import java.time.LocalDate
import javax.inject.Inject
import pe.enrutatuagua.data.red.*
import pe.enrutatuagua.domain.modelo.*
import pe.enrutatuagua.domain.repositorio.OperacionRepository

class OperacionRepositoryHttp @Inject constructor(private val api:OperacionApi):OperacionRepository {
    override suspend fun resumen()=api.resumen().let { r->ResumenOperacion(r.asociaciones.map{AsociacionOperacion(it.id,it.nombre)},r.vehiculos.map{VehiculoOperacion(it.id,it.placa,it.capacidadLitros)},r.aguateros.map{AguateroOperacion(it.id,it.nombre)}) }
    override suspend fun sugerencia(asociacionId:Long,vehiculoId:Long)=api.sugerencia(asociacionId,vehiculoId).let { SugerenciaRuta(it.capacidadLitros,it.litrosSugeridos,it.sugeridos.map(::pedido),it.disponibles.map(::pedido)) }
    override suspend fun planificar(asociacionId:Long,vehiculoId:Long,aguateroId:Long,fecha:LocalDate,pedidoIds:List<Long>)=ruta(api.planificar(PlanificarRutaRequestDto(asociacionId,vehiculoId,aguateroId,fecha.toString(),pedidoIds)))
    override suspend fun cancelar(rutaId:Long)=api.cancelar(rutaId)
    override suspend fun asignada()=ruta(api.asignada())
    override suspend fun iniciar(rutaId:Long)=ruta(api.iniciar(rutaId))
    private fun pedido(d:PedidoOperacionDto)=PedidoOperacion(d.id,d.pedidoItemId,d.puntoId,d.puntoNombre,d.litros)
    private fun ruta(d:RutaPlanificadaDto)=RutaPlanificada(d.id,LocalDate.parse(d.fecha),d.asociacionId,d.vehiculoId,d.aguateroId,d.litros,EstadoRuta.entries.first{it.codigo==d.estado})
}
