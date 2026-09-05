package pe.enrutatuagua.app

import pe.enrutatuagua.app.ubicacion.AnclaTracking
import pe.enrutatuagua.app.ubicacion.ProveedorAnclaTracking
import pe.enrutatuagua.app.ubicacion.ResultadoAnclaTracking
import pe.enrutatuagua.domain.modelo.FuenteCarga
import pe.enrutatuagua.domain.modelo.IncidenciaOperativa
import pe.enrutatuagua.domain.modelo.RecepcionPendiente
import pe.enrutatuagua.domain.modelo.SolicitudRecarga
import pe.enrutatuagua.domain.repositorio.CustodiaAguaRepository

object CustodiaAguaRepoFake : CustodiaAguaRepository {
    var paginaPendientes = pe.enrutatuagua.domain.modelo.PaginaRecepciones<RecepcionPendiente>(emptyList(),0,20,0,0)
    var paginaHistorial = pe.enrutatuagua.domain.modelo.PaginaRecepciones<pe.enrutatuagua.domain.modelo.RecepcionAgua>(emptyList(),0,20,0,0)
    val mensajesConfirmacion = mutableListOf<String>()
    var fallosConfirmacion = 0
    fun reiniciar() {
        paginaPendientes=pe.enrutatuagua.domain.modelo.PaginaRecepciones(emptyList(),0,20,0,0)
        paginaHistorial=pe.enrutatuagua.domain.modelo.PaginaRecepciones(emptyList(),0,20,0,0)
        mensajesConfirmacion.clear();fallosConfirmacion=0
    }
    override suspend fun fuentes(): List<FuenteCarga> = emptyList()
    override suspend fun solicitarRecarga(mensajeId: String, fuenteId: Long, litros: Int, motivo: String,
        lat: Double?, lng: Double?, precisionMetros: Float?): SolicitudRecarga = error("No configurado")
    override suspend fun solicitudActiva(): SolicitudRecarga? = null
    override suspend fun reportarCarga(id: String, litros: Int, lat: Double?, lng: Double?,
        precisionMetros: Float?): SolicitudRecarga = error("No configurado")
    override suspend fun solicitudes(estado: Int?): List<SolicitudRecarga> = emptyList()
    override suspend fun autorizar(id: String, contacto: String, nota: String?): SolicitudRecarga = error("No configurado")
    override suspend fun confirmarDespacho(id: String, litros: Int, contacto: String, nota: String?): SolicitudRecarga = error("No configurado")
    override suspend fun rechazar(id: String, motivo: String): SolicitudRecarga = error("No configurado")
    override suspend fun recepcionesPendientes(): List<RecepcionPendiente> = emptyList()
    override suspend fun recepcionesPendientes(pagina: Int, tamanio: Int) = paginaPendientes
    override suspend fun historialRecepciones(pagina: Int, tamanio: Int) = paginaHistorial
    override suspend fun recepcionesOperador(estado: Int?, pagina: Int, tamanio: Int) = paginaHistorial
    override suspend fun confirmarRecepcion(mensajeId: String, rutaPuntoId: Long, litrosRecibidos: Int, comentario: String?) {
        mensajesConfirmacion += mensajeId
        if(fallosConfirmacion>0){fallosConfirmacion--;error("Sin conexión")}
    }
    override suspend fun incidencias(estado: Int?): List<IncidenciaOperativa> = emptyList()
    override suspend fun resolverIncidencia(id: String, estado: Int, resolucion: String): IncidenciaOperativa = error("No configurado")
}

object UbicacionFake : ProveedorAnclaTracking {
    override suspend fun obtener(): ResultadoAnclaTracking = ResultadoAnclaTracking.Disponible(
        AnclaTracking(-12.0464, -77.0428, System.currentTimeMillis(), 5f),
    )
}
