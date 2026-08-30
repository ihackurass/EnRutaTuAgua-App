package pe.enrutatuagua.app.ui.ruta

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.tokens.*
import pe.enrutatuagua.domain.modelo.RutaPlanificada
import pe.enrutatuagua.domain.repositorio.OperacionRepository
import pe.enrutatuagua.domain.repositorio.RutaRepository
import retrofit2.HttpException

@HiltViewModel class RutaAsignadaViewModel @Inject constructor(private val repo:OperacionRepository,private val rutas:RutaRepository):ViewModel(){
    val ruta=MutableStateFlow<RutaPlanificada?>(null);val cargando=MutableStateFlow(true);val mensaje=MutableStateFlow<String?>(null)
    init{viewModelScope.launch { while (true) { consultarAhora(); delay(5_000L) } }}
    fun consultar()=viewModelScope.launch{cargando.value=true;mensaje.value=null;try{ruta.value=repo.asignada()}catch(e:HttpException){ruta.value=null;if(e.code()!=404)mensaje.value="No se pudo consultar la ruta"}catch(_:Exception){ruta.value=null;mensaje.value="No se pudo consultar la ruta"}finally{cargando.value=false}}
    private suspend fun consultarAhora(){try{ruta.value=repo.asignada();mensaje.value=null}catch(e:HttpException){ruta.value=null;if(e.code()!=404)mensaje.value="No se pudo consultar la ruta"}catch(_:Exception){mensaje.value="No se pudo consultar la ruta"}finally{cargando.value=false}}
    fun iniciar()=viewModelScope.launch{val r=ruta.value?:return@launch;try{repo.iniciar(r.id);rutas.invalidarCache();ruta.value=null}catch(e:Exception){mensaje.value=e.message?:"No se pudo iniciar"}}
}

@Composable fun RutaAsignadaGate(conRutaAbierta: @Composable () -> Unit,vm:RutaAsignadaViewModel= hiltViewModel()){
    val r by vm.ruta.collectAsState();val cargando by vm.cargando.collectAsState();val m by vm.mensaje.collectAsState();val p=LocalPaletaAgua.current
    if(r==null&&!cargando&&m==null){conRutaAbierta();return}
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Text(if(cargando)"Consultando ruta…" else "Ruta #${r?.id} asignada",style=EstilosAgua.TituloParada,color=p.texto)
        r?.let{Text("${it.litros} L · ${it.fecha}",color=p.texto2);Spacer(Modifier.height(16.dp));BotonPrimario("Iniciar ruta",onClick=vm::iniciar)}
        m?.let{Text(it,color=p.error);Spacer(Modifier.height(12.dp));BotonPrimario("Reintentar",onClick=vm::consultar)}
    }}
}
