package pe.enrutatuagua.data

import javax.inject.Inject
import pe.enrutatuagua.data.red.*
import pe.enrutatuagua.domain.modelo.*
import pe.enrutatuagua.domain.repositorio.CustodiaAguaRepository

class CustodiaAguaRepositoryHttp @Inject constructor(private val api:CustodiaAguaApi):CustodiaAguaRepository {
    override suspend fun fuentes()=api.fuentes().map { FuenteCarga(it.id,it.nombre,it.lat,it.lng,it.radioPermitidoMetros,it.contactoNombre,it.contactoTelefono) }
    override suspend fun solicitarRecarga(mensajeId:String,fuenteId:Long,litros:Int,motivo:String,lat:Double?,lng:Double?,precisionMetros:Float?)=
        api.solicitar(SolicitarRecargaDto(mensajeId,fuenteId,litros,motivo,lat,lng,precisionMetros)).dominio()
    override suspend fun solicitudActiva()=api.activa().let { if(it.isSuccessful)it.body()?.dominio() else null }
    override suspend fun reportarCarga(id:String,litros:Int,lat:Double?,lng:Double?,precisionMetros:Float?)=
        api.reportar(id,ReportarCargaDto(litros,lat,lng,precisionMetros)).dominio()
    override suspend fun solicitudes(estado:Int?)=api.solicitudes(estado).map { it.dominio() }
    override suspend fun autorizar(id:String,contacto:String,nota:String?)=api.autorizar(id,LlamadaOperadorDto(contacto,nota)).dominio()
    override suspend fun confirmarDespacho(id:String,litros:Int,contacto:String,nota:String?)=
        api.confirmar(id,ConfirmarDespachoDto(litros,contacto,nota)).dominio()
    override suspend fun rechazar(id:String,motivo:String)=api.rechazar(id,RechazarRecargaDto(motivo)).dominio()
    override suspend fun recepcionesPendientes()=api.recepciones().map { RecepcionPendiente(it.rutaPuntoId,it.rutaId,it.puntoNombre,it.litrosEsperados,it.litrosDeclarados,it.entregadaEn,it.venceEn) }
    override suspend fun recepcionesPendientes(pagina:Int,tamanio:Int)=api.recepcionesPaginadas(pagina,tamanio).let { p ->
        PaginaRecepciones(p.elementos.map { it.pendiente() },p.pagina,p.tamanio,p.totalElementos,p.totalPaginas)
    }
    override suspend fun historialRecepciones(pagina:Int,tamanio:Int)=api.historialRecepciones(pagina,tamanio).dominio()
    override suspend fun recepcionesOperador(estado:Int?,pagina:Int,tamanio:Int)=api.recepcionesOperador(estado,pagina,tamanio).dominio()
    override suspend fun confirmarRecepcion(mensajeId:String,rutaPuntoId:Long,litrosRecibidos:Int,comentario:String?) {
        api.confirmarRecepcion(rutaPuntoId,ConfirmarRecepcionDto(mensajeId,litrosRecibidos,comentario))
    }
    override suspend fun incidencias(estado:Int?)=api.incidencias(estado).map { IncidenciaOperativa(it.id,it.jornadaId,it.rutaId,it.tipo,it.severidad,it.estado,it.referencia,it.detalle,it.creadaEn,it.resolucion) }
    override suspend fun resolverIncidencia(id:String,estado:Int,resolucion:String)=api.resolver(id,ResolverIncidenciaDto(estado,resolucion)).let {
        IncidenciaOperativa(it.id,it.jornadaId,it.rutaId,it.tipo,it.severidad,it.estado,it.referencia,it.detalle,it.creadaEn,it.resolucion)
    }
    private fun SolicitudRecargaDto.dominio()=SolicitudRecarga(id,jornadaId,rutaId,fuenteId,fuenteNombre,aguateroNombre,
        EstadoSolicitudRecarga.desdeCodigo(estado),litrosSolicitados,litrosReportados,litrosConfirmados,motivo,
        contactoAutorizacion,contactoConfirmacion,notaOperador,distanciaFuenteMetros,solicitadaEn,autorizacionExpiraEn)
    private fun RecepcionPendienteDto.pendiente()=RecepcionPendiente(rutaPuntoId,rutaId,puntoNombre,litrosEsperados,litrosDeclarados,entregadaEn,venceEn)
    private fun PaginaRecepcionesDto.dominio()=PaginaRecepciones(elementos.map { it.dominio() },pagina,tamanio,totalElementos,totalPaginas)
    private fun RecepcionDetalleDto.dominio()=RecepcionAgua(id,rutaPuntoId,rutaId,puntoNombre,aguateroNombre,
        litrosDeclarados,litrosRecibidos,estado,confirmadoPorNombre,comentario,confirmadaEn)
}
