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

data class OperacionUiState(
    val cargando:Boolean=true,val resumen:ResumenOperacion?=null,val asociacionId:Long?=null,
    val vehiculoId:Long?=null,val aguateroId:Long?=null,val fecha:LocalDate=LocalDate.now(),
    val disponibles:List<PedidoOperacion> = emptyList(),val seleccionados:Set<Long> = emptySet(),
    val capacidad:Int=0,val guardando:Boolean=false,val mensaje:String?=null,
)

@HiltViewModel
class OperacionViewModel @Inject constructor(private val repo:OperacionRepository):ViewModel(){
    private val _estado=MutableStateFlow(OperacionUiState()); val estado:StateFlow<OperacionUiState> = _estado
    init{ cargar() }
    fun cargar()=viewModelScope.launch{ ejecutar { val r=repo.resumen(); _estado.value=_estado.value.copy(cargando=false,resumen=r,asociacionId=r.asociaciones.firstOrNull()?.id,vehiculoId=r.vehiculos.firstOrNull()?.id,aguateroId=r.aguateros.firstOrNull()?.id); sugerir() } }
    fun elegirAsociacion(id:Long){_estado.value=_estado.value.copy(asociacionId=id); sugerirAsync()}
    fun elegirVehiculo(id:Long){_estado.value=_estado.value.copy(vehiculoId=id); sugerirAsync()}
    fun elegirAguatero(id:Long){_estado.value=_estado.value.copy(aguateroId=id)}
    fun elegirFecha(fecha:LocalDate){_estado.value=_estado.value.copy(fecha=fecha)}
    fun alternar(id:Long){val s=_estado.value.seleccionados.toMutableSet();if(!s.add(id))s.remove(id);_estado.value=_estado.value.copy(seleccionados=s,mensaje=null)}
    fun planificar()=viewModelScope.launch{val e=_estado.value;val a=e.asociacionId?:return@launch;val v=e.vehiculoId?:return@launch;val u=e.aguateroId?:return@launch;if(litrosSeleccionados()>e.capacidad){_estado.value=e.copy(mensaje="La selección supera la capacidad");return@launch};_estado.value=e.copy(guardando=true);ejecutar{val ruta=repo.planificar(a,v,u,e.fecha,e.seleccionados.toList());_estado.value=_estado.value.copy(guardando=false,mensaje="Ruta #${ruta.id} planificada",seleccionados=emptySet());sugerir()}}
    fun litrosSeleccionados()=_estado.value.disponibles.filter{it.id in _estado.value.seleccionados}.sumOf{it.litros}
    private fun sugerirAsync(){viewModelScope.launch{ejecutar{sugerir()}}}
    private suspend fun sugerir(){val e=_estado.value;val a=e.asociacionId?:return;val v=e.vehiculoId?:return;val s=repo.sugerencia(a,v);_estado.value=_estado.value.copy(disponibles=s.disponibles,seleccionados=s.sugeridos.map{it.id}.toSet(),capacidad=s.capacidadLitros,mensaje=null)}
    private suspend fun ejecutar(b:suspend()->Unit){try{b()}catch(t:Throwable){_estado.value=_estado.value.copy(cargando=false,guardando=false,mensaje=t.message?:"No se pudo completar la operación")}}
}
