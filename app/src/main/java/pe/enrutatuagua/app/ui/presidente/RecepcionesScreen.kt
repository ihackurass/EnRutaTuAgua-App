package pe.enrutatuagua.app.ui.presidente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.component.BotonAtras
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.*
import pe.enrutatuagua.domain.modelo.RecepcionAgua
import pe.enrutatuagua.domain.modelo.RecepcionPendiente

@Composable
fun RecepcionesScreen(onHistorial:()->Unit,vm:RecepcionesViewModel) {
    val e by vm.estado.collectAsState()
    val p=LocalPaletaAgua.current
    var seleccionada by remember { mutableStateOf<RecepcionPendiente?>(null) }
    LaunchedEffect(e.confirmacionExitosa) {
        if(e.confirmacionExitosa!=null){seleccionada=null;vm.consumirConfirmacion()}
    }
    seleccionada?.let { r -> DialogoConfirmarRecepcion(r,r.rutaPuntoId in e.confirmando,e.error,
        onDismiss={if(r.rutaPuntoId !in e.confirmando){seleccionada=null;vm.limpiarError()}},
        onConfirmar={litros,comentario->vm.confirmar(r,litros,comentario)}) }

    LazyColumn(
        Modifier.fillMaxSize().background(p.fondo).statusBarsPadding(),
        contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),
    ) {
        item { CabeceraRecepciones(e.totalPendientes,onHistorial) }
        if(e.error!=null && seleccionada==null) item { TarjetaError(e.error!!) }
        when {
            e.cargandoPendientes && e.pendientes.isEmpty() -> item { CargandoRecepciones() }
            e.pendientes.isEmpty() -> item { EstadoSinRecepciones("No tienes entregas pendientes por confirmar.") }
            else -> items(e.pendientes,key={it.rutaPuntoId}) { r ->
                TarjetaRecepcionPendiente(r,r.rutaPuntoId in e.confirmando){seleccionada=r;vm.limpiarError()}
            }
        }
        if(e.paginaPendientes+1<e.paginasPendientes) item {
            TextButton(onClick=vm::cargarMasPendientes,enabled=!e.cargandoPendientes,
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                Text(if(e.cargandoPendientes)"Cargando…" else "Cargar más")
            }
        }
    }
}

@Composable
fun HistorialRecepcionesScreen(onVolver:()->Unit,vm:RecepcionesViewModel) {
    val e by vm.estado.collectAsState();val p=LocalPaletaAgua.current
    LaunchedEffect(Unit){vm.cargarHistorial()}
    LazyColumn(Modifier.fillMaxSize().background(p.fondo).statusBarsPadding(),
        contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { BotonAtras(onClick=onVolver);Spacer(Modifier.height(14.dp));Text("RECEPCIONES",style=EstilosAgua.Rotulo,color=p.texto2);
            Text("Historial de recepciones",style=EstilosAgua.TituloParada,color=p.texto,modifier=Modifier.semantics{heading()});
            Text("Confirmaciones registradas por tu comunidad.",style=EstilosAgua.Cuerpo,color=p.texto2) }
        if(e.error!=null)item{TarjetaError(e.error!!)}
        when {
            e.cargandoHistorial && e.historial.isEmpty()->item{CargandoRecepciones()}
            e.historial.isEmpty()->item{EstadoSinRecepciones("Todavía no hay recepciones confirmadas.")}
            else->items(e.historial,key={it.id}){TarjetaRecepcionHistorial(it)}
        }
        if(e.paginaHistorial+1<e.paginasHistorial)item{TextButton(onClick={vm.cargarHistorial(false)},
            enabled=!e.cargandoHistorial,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
            Text(if(e.cargandoHistorial)"Cargando…" else "Cargar más")}}
    }
}

@Composable private fun CabeceraRecepciones(total:Long,onHistorial:()->Unit){val p=LocalPaletaAgua.current
    Column{Text("PRESIDENCIA DE LA ZONA",style=EstilosAgua.Rotulo,color=p.texto2);Text("Recepciones por confirmar",
        style=EstilosAgua.TituloParada,color=p.texto,modifier=Modifier.semantics{heading()});
        Text("Verifica cuánto recibió realmente cada punto.",style=EstilosAgua.Cuerpo,color=p.texto2);Spacer(Modifier.height(12.dp));
        Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(RadiosAgua.Aviso),color=p.tinta){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("PENDIENTES",style=EstilosAgua.Rotulo,color=p.sobreTinta.copy(alpha=.75f));Text(total.toString(),style=EstilosAgua.DatoResumen,color=p.sobreTinta)};TextButton(onClick=onHistorial){Text("Ver historial",color=p.accion)}}}}
}

@Composable private fun TarjetaRecepcionPendiente(r:RecepcionPendiente,enviando:Boolean,onConfirmar:()->Unit){val p=LocalPaletaAgua.current
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(RadiosAgua.Aviso),color=p.tenue){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
        Text("RUTA #${r.rutaId}",style=EstilosAgua.Rotulo,color=p.accion);Text(r.puntoNombre,style=EstilosAgua.ItemTitulo,color=p.texto);
        Text("El aguatero declaró ${formatearLitros(r.litrosDeclarados)} L",style=EstilosAgua.Cuerpo,color=p.texto2)
        TextButton(onClick=onConfirmar,enabled=!enviando,modifier=Modifier.align(Alignment.End).heightIn(min=48.dp)){
            if(enviando){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Spacer(Modifier.width(8.dp))};Text(if(enviando)"Confirmando…" else "Indicar litros recibidos")}
    }}
}

@Composable private fun DialogoConfirmarRecepcion(r:RecepcionPendiente,enviando:Boolean,error:String?,onDismiss:()->Unit,onConfirmar:(Int,String?)->Unit){
    var litros by remember(r.rutaPuntoId){mutableStateOf(r.litrosDeclarados.toString())};var comentario by remember(r.rutaPuntoId){mutableStateOf("")}
    val cantidad=litros.toIntOrNull();val diferencia=cantidad!=null&&cantidad!=r.litrosDeclarados
    AlertDialog(onDismissRequest=onDismiss,title={Text("Confirmar recepción")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Registra la cantidad que realmente recibió ${r.puntoNombre}.")
        OutlinedTextField(value=litros,onValueChange={if(!enviando)litros=it.filter(Char::isDigit).take(6)},enabled=!enviando,
            label={Text("Litros realmente recibidos")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
        if(diferencia)OutlinedTextField(value=comentario,onValueChange={if(!enviando)comentario=it.take(250)},enabled=!enviando,
            label={Text("Explique la diferencia")},minLines=2)
        error?.let{Text(it,style=EstilosAgua.Error,color=LocalPaletaAgua.current.error)}
    }},confirmButton={TextButton(enabled=!enviando&&cantidad!=null&&(!diferencia||comentario.trim().length>=5),
        onClick={onConfirmar(checkNotNull(cantidad),comentario.trim().ifBlank{null})}){Text(if(enviando)"Confirmando…" else "Confirmar litros")}},
        dismissButton={TextButton(onClick=onDismiss,enabled=!enviando){Text("Cancelar")}})
}

@Composable private fun TarjetaRecepcionHistorial(r:RecepcionAgua){val p=LocalPaletaAgua.current;val diferencia=r.estado==3
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(RadiosAgua.Aviso),color=if(diferencia)p.error.copy(alpha=.08f) else p.tenue){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
        Row{Text(if(diferencia)"CON DIFERENCIA" else "CONFORME",style=EstilosAgua.Rotulo,color=if(diferencia)p.error else p.exito,modifier=Modifier.weight(1f));Text("Ruta #${r.rutaId}",style=EstilosAgua.ItemSub,color=p.texto2)}
        Text(r.puntoNombre,style=EstilosAgua.ItemTitulo,color=p.texto);Text("Declarados ${formatearLitros(r.litrosDeclarados)} L · Recibidos ${formatearLitros(r.litrosRecibidos)} L",style=EstilosAgua.Cuerpo,color=p.texto2)
        r.comentario?.let{Text(it,style=EstilosAgua.ItemSub,color=p.texto2)};Text("Confirmó ${r.confirmadoPorNombre}",style=EstilosAgua.ItemSub,color=p.texto2)
    }}
}

@Composable private fun CargandoRecepciones(){Box(Modifier.fillMaxWidth().height(140.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}}
@Composable private fun EstadoSinRecepciones(texto:String){val p=LocalPaletaAgua.current;Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(RadiosAgua.Aviso),color=p.tenue){Text(texto,Modifier.padding(20.dp),style=EstilosAgua.Cuerpo,color=p.texto2)}}
