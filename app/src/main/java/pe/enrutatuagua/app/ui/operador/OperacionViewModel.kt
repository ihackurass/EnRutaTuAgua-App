package pe.enrutatuagua.app.ui.operador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pe.enrutatuagua.domain.modelo.*
import pe.enrutatuagua.domain.repositorio.OperacionRepository
import pe.enrutatuagua.domain.repositorio.CustodiaAguaRepository
import pe.enrutatuagua.data.red.mensajeUsuario

data class OperacionUiState(
    val cargando:Boolean=true,val resumen:ResumenOperacion?=null,val asociacionId:Long?=null,
    val vehiculoId:Long?=null,val aguateroId:Long?=null,val fecha:LocalDate=LocalDate.now(),
    val disponibles:List<PedidoOperacion> = emptyList(),val seleccionados:Set<Long> = emptySet(),
    val capacidad:Int=0,val guardando:Boolean=false,val mensaje:String?=null,
    val rutas:List<RutaOperacion> = emptyList(), val eventos:List<EventoOperacion> = emptyList(), val confirmar:Boolean=false,
    val recargas:List<SolicitudRecarga> = emptyList(), val incidencias:List<IncidenciaOperativa> = emptyList(),
    val recepciones:List<RecepcionAgua> = emptyList(), val filtroRecepcion:Int? = null,
)

@HiltViewModel
class OperacionViewModel @Inject constructor(private val repo:OperacionRepository,private val custodia:CustodiaAguaRepository):ViewModel(){
    private val _estado=MutableStateFlow(OperacionUiState()); val estado:StateFlow<OperacionUiState> = _estado
    init{ cargar() }
    fun cargar()=viewModelScope.launch{ ejecutar { cargarRecursos(_estado.value.fecha, true) } }
    fun elegirAsociacion(id:Long){_estado.value=_estado.value.copy(asociacionId=id); sugerirAsync()}
    fun elegirVehiculo(id:Long){val conductor=_estado.value.resumen?.vehiculos?.firstOrNull{it.id==id}?.conductorId;_estado.value=_estado.value.copy(vehiculoId=id,aguateroId=conductor); sugerirAsync()}
    fun elegirAguatero(id:Long){_estado.value=_estado.value.copy(aguateroId=id)}
    fun elegirFecha(fecha:LocalDate)=viewModelScope.launch{ejecutar{cargarRecursos(fecha, false)}}
    fun alternar(id:Long){val s=_estado.value.seleccionados.toMutableSet();if(!s.add(id))s.remove(id);_estado.value=_estado.value.copy(seleccionados=s,mensaje=null)}
    fun solicitarConfirmacion(){_estado.value=_estado.value.copy(confirmar=true)}
    fun cerrarConfirmacion(){_estado.value=_estado.value.copy(confirmar=false)}
    fun planificar()=viewModelScope.launch{val e=_estado.value;val a=e.asociacionId?:return@launch;val v=e.vehiculoId?:return@launch;val u=e.aguateroId?:return@launch;if(litrosSeleccionados()>e.capacidad){_estado.value=e.copy(mensaje="La selección supera la capacidad",confirmar=false);return@launch};_estado.value=e.copy(guardando=true,confirmar=false);ejecutar{val ruta=repo.planificar(a,v,u,e.fecha,e.seleccionados.toList());cargarRecursos(e.fecha,false);_estado.value=_estado.value.copy(guardando=false,mensaje="Ruta #${ruta.id} planificada",rutas=repo.listar())}}
    fun refrescarRutas()=viewModelScope.launch{ejecutar{_estado.value=_estado.value.copy(rutas=repo.listar())}}
    fun cargarAuditoria()=viewModelScope.launch{ejecutar{_estado.value=_estado.value.copy(eventos=repo.auditoria())}}
    fun cargarCustodia()=viewModelScope.launch{ejecutar{val e=_estado.value;_estado.value=e.copy(recargas=custodia.solicitudes(),incidencias=custodia.incidencias(),recepciones=custodia.recepcionesOperador(e.filtroRecepcion,0,20).elementos)}}
    fun filtrarRecepciones(estado:Int?)=viewModelScope.launch{ejecutar{_estado.value=_estado.value.copy(filtroRecepcion=estado,recepciones=custodia.recepcionesOperador(estado,0,20).elementos)}}
    fun autorizarRecarga(id:String,contacto:String)=accionCustodia{custodia.autorizar(id,contacto,"Primera llamada registrada")}
    fun confirmarRecarga(id:String,litros:Int,contacto:String)=accionCustodia{custodia.confirmarDespacho(id,litros,contacto,"Segunda llamada registrada")}
    fun rechazarRecarga(id:String,motivo:String)=accionCustodia{custodia.rechazar(id,motivo)}
    fun resolverIncidencia(id:String,resolucion:String)=accionCustodia{custodia.resolverIncidencia(id,3,resolucion)}
    fun cancelar(id:Long)=viewModelScope.launch{ejecutar{repo.cancelar(id);val fecha=_estado.value.fecha;cargarRecursos(fecha,false);_estado.value=_estado.value.copy(mensaje="Ruta #$id cancelada",rutas=repo.listar())}}
    fun litrosSeleccionados()=_estado.value.disponibles.filter{it.id in _estado.value.seleccionados}.sumOf{it.litros}
    private fun sugerirAsync(){viewModelScope.launch{ejecutar{sugerir()}}}
    private suspend fun cargarRecursos(fecha:LocalDate, cargarRutas:Boolean){
        val anterior=_estado.value
        val r=repo.resumen(fecha)
        val vehiculo=r.vehiculos.firstOrNull{it.disponible && it.id==anterior.vehiculoId}?:r.vehiculos.firstOrNull{it.disponible}
        val aguatero=r.aguateros.firstOrNull{it.disponible && it.id==vehiculo?.conductorId}
        _estado.value=anterior.copy(cargando=false,resumen=r,fecha=fecha,
            asociacionId=anterior.asociacionId?:r.asociaciones.firstOrNull()?.id,
            vehiculoId=vehiculo?.id,aguateroId=aguatero?.id,
            disponibles=emptyList(),seleccionados=emptySet(),capacidad=vehiculo?.capacidadLitros?:0,
            rutas=if(cargarRutas)repo.listar() else anterior.rutas,mensaje=null)
        if(vehiculo!=null)sugerir()
    }
    private suspend fun sugerir(){val e=_estado.value;val a=e.asociacionId?:return;val v=e.vehiculoId?:return;val s=repo.sugerencia(a,v);_estado.value=_estado.value.copy(disponibles=s.disponibles,seleccionados=s.sugeridos.map{it.id}.toSet(),capacidad=s.capacidadLitros,mensaje=null)}
    private suspend fun ejecutar(b:suspend()->Unit){try{b()}catch(t:Throwable){_estado.value=_estado.value.copy(cargando=false,guardando=false,mensaje=t.mensajeUsuario())}}
    private fun accionCustodia(b:suspend()->Unit)=viewModelScope.launch{ejecutar{b();val e=_estado.value;_estado.value=e.copy(recargas=custodia.solicitudes(),incidencias=custodia.incidencias(),recepciones=custodia.recepcionesOperador(e.filtroRecepcion,0,20).elementos,mensaje="Control actualizado")}}
}
