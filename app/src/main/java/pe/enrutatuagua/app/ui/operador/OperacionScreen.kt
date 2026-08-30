package pe.enrutatuagua.app.ui.operador

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.tokens.*

@Composable fun OperacionScreen(vm:OperacionViewModel= hiltViewModel()){
    val e by vm.estado.collectAsState();val p=LocalPaletaAgua.current
    LazyColumn(Modifier.fillMaxSize().padding(18.dp).statusBarsPadding().navigationBarsPadding(),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("Planificación de rutas",style=EstilosAgua.TituloParada,color=p.texto);Text("La empresa prepara la cisterna antes de la salida",color=p.texto2)}
        e.resumen?.let{r->
            item{Text("Asociación",style=EstilosAgua.ItemTitulo);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){r.asociaciones.forEach{FilterChip(it.id==e.asociacionId,{vm.elegirAsociacion(it.id)},{Text(it.nombre)})}}}
            item{Text("Cisterna",style=EstilosAgua.ItemTitulo);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){r.vehiculos.forEach{FilterChip(it.id==e.vehiculoId,{vm.elegirVehiculo(it.id)},{Text("${it.placa} · ${it.capacidadLitros} L")})}}}
            item{Text("Aguatero",style=EstilosAgua.ItemTitulo);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){r.aguateros.forEach{FilterChip(it.id==e.aguateroId,{vm.elegirAguatero(it.id)},{Text(it.nombre)})}}}
            item{Text("Fecha",style=EstilosAgua.ItemTitulo);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(LocalDate.now(),LocalDate.now().plusDays(1)).forEach{FilterChip(it==e.fecha,{vm.elegirFecha(it)},{Text(if(it==LocalDate.now())"Hoy" else "Mañana")})}}}
        }
        item{Text("Pedidos sugeridos",style=EstilosAgua.ItemTitulo);Text("${vm.litrosSeleccionados()} / ${e.capacidad} L",color=if(vm.litrosSeleccionados()>e.capacidad)p.error else p.accion)}
        items(e.disponibles,key={it.id}){pedido->Row(Modifier.fillMaxWidth().clickable{vm.alternar(pedido.id)}.padding(vertical=6.dp)){Checkbox(pedido.id in e.seleccionados,{vm.alternar(pedido.id)});Column{Text(pedido.puntoNombre,color=p.texto);Text("Pedido #${pedido.id} · ${pedido.litros} L",color=p.texto2)}}}
        e.mensaje?.let{item{Text(it,color=if(it.startsWith("Ruta"))p.accion else p.error)}}
        item{BotonPrimario(if(e.guardando)"Planificando…" else "Confirmar planificación",habilitado=e.seleccionados.isNotEmpty()&&!e.guardando&&vm.litrosSeleccionados()<=e.capacidad,onClick=vm::planificar)}
    }
}
