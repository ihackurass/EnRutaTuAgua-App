package pe.enrutatuagua.app.ui.operador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.*
import pe.enrutatuagua.domain.modelo.*

private enum class VistaOperacion { PLANIFICAR, RUTAS }

@Composable fun OperacionScreen(vm: OperacionViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsState(); val p = LocalPaletaAgua.current
    var vista by rememberSaveable { mutableStateOf(VistaOperacion.PLANIFICAR) }
    var cancelarRuta by remember { mutableStateOf<Long?>(null) }
    if (e.confirmar) ConfirmarPlanificacion(e, vm)
    cancelarRuta?.let { id -> AlertDialog(onDismissRequest = { cancelarRuta = null }, title = { Text("Cancelar ruta #$id") }, text = { Text("Los pedidos volverán a quedar disponibles para otra planificación.") }, confirmButton = { TextButton(onClick = { vm.cancelar(id); cancelarRuta = null }) { Text("Sí, cancelar", color = p.error) } }, dismissButton = { TextButton(onClick = { cancelarRuta = null }) { Text("Conservar ruta") } }) }

    LazyColumn(
        Modifier.fillMaxSize().background(p.hoja).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("OPERACIÓN · ${LocalDate.now()}", style = EstilosAgua.Rotulo, color = p.texto2); Spacer(Modifier.height(5.dp)); Text(if (vista == VistaOperacion.PLANIFICAR) "Planificar reparto" else "Rutas de la empresa", style = EstilosAgua.TituloParada, color = p.texto); Text(if (vista == VistaOperacion.PLANIFICAR) "Asigna la demanda a una cisterna" else "Consulta el estado de cada reparto", style = EstilosAgua.Cuerpo, color = p.texto2) }
        item { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(RadiosAgua.Aviso)).background(p.tenue).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { TabOperacion("Planificar", vista == VistaOperacion.PLANIFICAR, Modifier.weight(1f)) { vista = VistaOperacion.PLANIFICAR }; TabOperacion("Rutas", vista == VistaOperacion.RUTAS, Modifier.weight(1f)) { vista = VistaOperacion.RUTAS; vm.refrescarRutas() } } }
        if (vista == VistaOperacion.PLANIFICAR) contenidoPlanificar(e, vm) else contenidoRutas(e, vm, onCancelar = { cancelarRuta = it })
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.contenidoPlanificar(e: OperacionUiState, vm: OperacionViewModel) {
    item { ResumenCapacidad(vm.litrosSeleccionados(), e.capacidad, e.seleccionados.size) }
    e.resumen?.let { r ->
        item { SelectorHorizontal("Asociación", r.asociaciones, e.asociacionId, { it.id }, { it.nombre }, vm::elegirAsociacion) }
        item { SelectorHorizontal("Cisterna", r.vehiculos, e.vehiculoId, { it.id }, { "${it.placa} · ${it.capacidadLitros} L" }, vm::elegirVehiculo) }
        item { SelectorHorizontal("Aguatero", r.aguateros, e.aguateroId, { it.id }, { it.nombre }, vm::elegirAguatero) }
        item { SelectorHorizontal("Fecha", listOf(LocalDate.now(), LocalDate.now().plusDays(1)), e.fecha, { it }, { if (it == LocalDate.now()) "Hoy" else "Mañana" }, vm::elegirFecha) }
    }
    item { val p = LocalPaletaAgua.current; Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("PEDIDOS SUGERIDOS", style = EstilosAgua.Rotulo, color = p.texto2, modifier = Modifier.weight(1f)); Text("${e.seleccionados.size} elegidos", style = EstilosAgua.ItemSub, color = p.accion) } }
    if (e.disponibles.isEmpty()) item { val p = LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Aviso), color = p.tenue) { Text("No hay pedidos disponibles para esta selección.", modifier = Modifier.padding(16.dp), style = EstilosAgua.Cuerpo, color = p.texto2) } }
    items(e.disponibles, key = { it.id }) { pedido -> PedidoSeleccionable(pedido, pedido.id in e.seleccionados) { vm.alternar(pedido.id) } }
    e.mensaje?.let { item { MensajeOperacion(it) } }
    item { BotonPrimario(if (e.guardando) "Planificando…" else "Revisar y crear", habilitado = e.seleccionados.isNotEmpty() && !e.guardando && vm.litrosSeleccionados() <= e.capacidad, onClick = vm::solicitarConfirmacion) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.contenidoRutas(e: OperacionUiState, vm: OperacionViewModel, onCancelar: (Long) -> Unit) {
    item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { val p = LocalPaletaAgua.current; Text("HISTORIAL OPERATIVO", style = EstilosAgua.Rotulo, color = p.texto2, modifier = Modifier.weight(1f)); TextButton(onClick = vm::refrescarRutas) { Text("Actualizar", color = p.accion) } } }
    if (e.rutas.isEmpty()) item { val p = LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Aviso), color = p.tenue) { Text("Todavía no hay rutas para mostrar.", Modifier.padding(18.dp), color = p.texto2) } }
    items(e.rutas, key = { it.id }) { ruta -> RutaOperacionCard(ruta, onCancelar) }
    e.mensaje?.let { item { MensajeOperacion(it) } }
}

@Composable private fun TabOperacion(texto: String, elegido: Boolean, modifier: Modifier, onClick: () -> Unit) { val p = LocalPaletaAgua.current; Surface(modifier.heightIn(min = 46.dp).clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), color = if (elegido) p.tinta else p.tenue, contentColor = if (elegido) p.sobreTinta else p.texto2) { Box(contentAlignment = Alignment.Center) { Text(texto, style = EstilosAgua.ItemTitulo) } } }

@Composable private fun ResumenCapacidad(litros: Int, capacidad: Int, pedidos: Int) { val p = LocalPaletaAgua.current; val proporcion = if (capacidad > 0) (litros.toFloat() / capacidad).coerceIn(0f, 1f) else 0f; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Hoja), color = p.tinta) { Column(Modifier.padding(18.dp)) { Text("DEMANDA SELECCIONADA", style = EstilosAgua.Rotulo, color = p.sobreTinta.copy(alpha = .7f)); Spacer(Modifier.height(6.dp)); Text("${formatearLitros(litros)} de ${formatearLitros(capacidad)} L", style = EstilosAgua.DatoResumen, color = p.sobreTinta); Text("$pedidos pedidos · quedan ${formatearLitros((capacidad-litros).coerceAtLeast(0))} L", style = EstilosAgua.ItemSub, color = p.sobreTinta.copy(alpha = .75f)); Spacer(Modifier.height(12.dp)); Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(p.sobreTinta.copy(alpha = .2f))) { Box(Modifier.fillMaxWidth(proporcion).fillMaxHeight().background(p.accion)) } } } }

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun <T, K> SelectorHorizontal(titulo: String, opciones: List<T>, elegido: K?, id: (T) -> K, texto: (T) -> String, elegir: (K) -> Unit) { val p = LocalPaletaAgua.current; Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(titulo, style = EstilosAgua.ItemTitulo, color = p.texto); FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { opciones.forEach { opcion -> FilterChip(selected = id(opcion) == elegido, onClick = { elegir(id(opcion)) }, label = { Text(texto(opcion), maxLines = 1) }, colors = FilterChipDefaults.filterChipColors(containerColor = p.hoja, labelColor = p.texto2, selectedContainerColor = p.superficie2, selectedLabelColor = p.texto)) } } } }

@Composable private fun PedidoSeleccionable(pedido: PedidoOperacion, elegido: Boolean, onClick: () -> Unit) { val p = LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(RadiosAgua.Aviso), color = if (elegido) p.tenue else p.hoja) { Row(Modifier.padding(vertical = 10.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(elegido, onCheckedChange = { onClick() }); Column(Modifier.weight(1f)) { Text(pedido.puntoNombre, style = EstilosAgua.ItemTitulo, color = p.texto); Text("Pedido #${pedido.id} · ${pedido.litros} L", style = EstilosAgua.ItemSub, color = p.texto2) } } } }

@Composable private fun RutaOperacionCard(ruta: RutaOperacion, onCancelar: (Long) -> Unit) { val p = LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Aviso), color = p.tenue) { Column(Modifier.padding(16.dp)) { Row { Column(Modifier.weight(1f)) { Text("Ruta #${ruta.id}", style = EstilosAgua.ItemTitulo, color = p.texto); Text(ruta.asociacion.nombre, style = EstilosAgua.Cuerpo, color = p.texto2) }; SuggestionChip(onClick = {}, label = { Text(ruta.estado.etiqueta()) }) }; Spacer(Modifier.height(7.dp)); Text("${ruta.vehiculo.placa} · ${ruta.aguatero.nombre}", style = EstilosAgua.ItemTitulo, color = p.texto); Text("${ruta.fecha} · ${ruta.litros} L · ${ruta.paradas} paradas", style = EstilosAgua.ItemSub, color = p.texto2); if (ruta.estado == EstadoRuta.PLANIFICADA) TextButton(onClick = { onCancelar(ruta.id) }, modifier = Modifier.align(Alignment.End)) { Text("Cancelar ruta", color = p.error) } } } }

@Composable private fun MensajeOperacion(mensaje: String) { val p = LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Aviso), color = if (mensaje.startsWith("Ruta")) p.exito.copy(alpha = .12f) else p.alerta.copy(alpha = .12f)) { Text(mensaje, Modifier.padding(12.dp), style = EstilosAgua.Error, color = if (mensaje.startsWith("Ruta")) p.exito else p.alerta) } }

@Composable private fun ConfirmarPlanificacion(e: OperacionUiState, vm: OperacionViewModel) { val r = e.resumen; val a = r?.asociaciones?.find { it.id == e.asociacionId }; val v = r?.vehiculos?.find { it.id == e.vehiculoId }; val u = r?.aguateros?.find { it.id == e.aguateroId }; AlertDialog(onDismissRequest = vm::cerrarConfirmacion, title = { Text("Revisar planificación") }, text = { Text("${a?.nombre}\n${v?.placa} · ${v?.capacidadLitros} L\n${u?.nombre} · ${e.fecha}\n${e.seleccionados.size} pedidos · ${vm.litrosSeleccionados()} L") }, confirmButton = { TextButton(onClick = vm::planificar) { Text("Crear ruta") } }, dismissButton = { TextButton(onClick = vm::cerrarConfirmacion) { Text("Volver") } }) }

private fun EstadoRuta.etiqueta() = when(this) { EstadoRuta.ABIERTA -> "Abierta"; EstadoRuta.CERRADA -> "Cerrada"; EstadoRuta.PLANIFICADA -> "Planificada"; EstadoRuta.CANCELADA -> "Cancelada" }
