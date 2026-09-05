package pe.enrutatuagua.app.ui.presidente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pe.enrutatuagua.data.red.mensajeUsuario
import pe.enrutatuagua.domain.modelo.RecepcionAgua
import pe.enrutatuagua.domain.modelo.RecepcionPendiente
import pe.enrutatuagua.domain.repositorio.CustodiaAguaRepository

data class RecepcionesUiState(
    val pendientes: List<RecepcionPendiente> = emptyList(),
    val historial: List<RecepcionAgua> = emptyList(),
    val totalPendientes: Long = 0,
    val paginaPendientes: Int = 0,
    val paginasPendientes: Int = 0,
    val paginaHistorial: Int = 0,
    val paginasHistorial: Int = 0,
    val cargandoPendientes: Boolean = true,
    val cargandoHistorial: Boolean = false,
    val confirmando: Set<Long> = emptySet(),
    val error: String? = null,
    val confirmacionExitosa: Long? = null,
)

@HiltViewModel
class RecepcionesViewModel @Inject constructor(
    private val custodia: CustodiaAguaRepository,
) : ViewModel() {
    private val _estado=MutableStateFlow(RecepcionesUiState())
    val estado:StateFlow<RecepcionesUiState> = _estado
    private val mensajes=mutableMapOf<Long,String>()

    init { refrescarPendientes() }

    fun refrescarPendientes()=viewModelScope.launch {
        _estado.value=_estado.value.copy(cargandoPendientes=true,error=null)
        try {
            val pagina=custodia.recepcionesPendientes(0,TAMANIO)
            _estado.value=_estado.value.copy(pendientes=pagina.elementos,totalPendientes=pagina.totalElementos,
                paginaPendientes=pagina.pagina,paginasPendientes=pagina.totalPaginas,cargandoPendientes=false)
        } catch(t:Throwable) { _estado.value=_estado.value.copy(cargandoPendientes=false,error=t.mensajeUsuario()) }
    }

    fun cargarMasPendientes()=viewModelScope.launch {
        val e=_estado.value
        if(e.cargandoPendientes || e.paginaPendientes+1>=e.paginasPendientes)return@launch
        _estado.value=e.copy(cargandoPendientes=true,error=null)
        try {
            val pagina=custodia.recepcionesPendientes(e.paginaPendientes+1,TAMANIO)
            _estado.value=_estado.value.copy(pendientes=e.pendientes+pagina.elementos,paginaPendientes=pagina.pagina,
                paginasPendientes=pagina.totalPaginas,totalPendientes=pagina.totalElementos,cargandoPendientes=false)
        } catch(t:Throwable) { _estado.value=_estado.value.copy(cargandoPendientes=false,error=t.mensajeUsuario()) }
    }

    fun cargarHistorial(reiniciar:Boolean=true)=viewModelScope.launch {
        val e=_estado.value
        val paginaSolicitada=if(reiniciar)0 else e.paginaHistorial+1
        if(e.cargandoHistorial || (!reiniciar && paginaSolicitada>=e.paginasHistorial))return@launch
        _estado.value=e.copy(cargandoHistorial=true,error=null)
        try {
            val pagina=custodia.historialRecepciones(paginaSolicitada,TAMANIO)
            _estado.value=_estado.value.copy(historial=if(reiniciar)pagina.elementos else e.historial+pagina.elementos,
                paginaHistorial=pagina.pagina,paginasHistorial=pagina.totalPaginas,cargandoHistorial=false)
        } catch(t:Throwable) { _estado.value=_estado.value.copy(cargandoHistorial=false,error=t.mensajeUsuario()) }
    }

    fun confirmar(recepcion:RecepcionPendiente,litros:Int,comentario:String?) {
        val id=recepcion.rutaPuntoId
        if(id in _estado.value.confirmando)return
        _estado.value=_estado.value.copy(confirmando=_estado.value.confirmando+id,error=null)
        viewModelScope.launch {
            try {
                val mensaje=mensajes.getOrPut(id){UUID.randomUUID().toString()}
                custodia.confirmarRecepcion(mensaje,id,litros,comentario)
                mensajes.remove(id)
                val restantes=_estado.value.pendientes.filterNot{it.rutaPuntoId==id}
                _estado.value=_estado.value.copy(pendientes=restantes,totalPendientes=(_estado.value.totalPendientes-1).coerceAtLeast(0),
                    confirmando=_estado.value.confirmando-id,confirmacionExitosa=id,error=null)
            } catch(t:Throwable) {
                _estado.value=_estado.value.copy(confirmando=_estado.value.confirmando-id,error=t.mensajeUsuario())
            }
        }
    }

    fun consumirConfirmacion() { _estado.value=_estado.value.copy(confirmacionExitosa=null) }
    fun limpiarError() { _estado.value=_estado.value.copy(error=null) }

    private companion object { const val TAMANIO=20 }
}
