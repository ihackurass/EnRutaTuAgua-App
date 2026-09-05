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

private enum class VistaOperacion { PLANIFICAR, RUTAS, CONTROL, AUDITORIA }

@Composable fun OperacionScreen(vm: OperacionViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsState(); val p = LocalPaletaAgua.current
    var vista by rememberSaveable { mutableStateOf(VistaOperacion.PLANIFICAR) }
    var cancelarRuta by remember { mutableStateOf<Long?>(null) }
    var accionRecarga by remember { mutableStateOf<Pair<SolicitudRecarga,String>?>(null) }
    var resolverIncidencia by remember { mutableStateOf<IncidenciaOperativa?>(null) }
    if (e.confirmar) ConfirmarPlanificacion(e, vm)
    cancelarRuta?.let { id -> AlertDialog(onDismissRequest = { cancelarRuta = null }, title = { Text("Cancelar ruta #$id") }, text = { Text("Los pedidos volverán a quedar disponibles para otra planificación.") }, confirmButton = { TextButton(onClick = { vm.cancelar(id); cancelarRuta = null }) { Text("Sí, cancelar", color = p.error) } }, dismissButton = { TextButton(onClick = { cancelarRuta = null }) { Text("Conservar ruta") } }) }
    accionRecarga?.let { (recarga,accion) -> DialogoControlRecarga(recarga,accion,{accionRecarga=null}) { contacto,valor ->
        when(accion){"autorizar"->vm.autorizarRecarga(recarga.id,contacto);"confirmar"->vm.confirmarRecarga(recarga.id,valor.toInt(),contacto);else->vm.rechazarRecarga(recarga.id,valor)}
        accionRecarga=null
    } }
    resolverIncidencia?.let { incidencia -> DialogoResolverIncidencia(incidencia,{resolverIncidencia=null}) { vm.resolverIncidencia(incidencia.id,it);resolverIncidencia=null } }

    LazyColumn(
        Modifier.fillMaxSize().background(p.hoja).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("OPERACIÓN · ${LocalDate.now()}", style = EstilosAgua.Rotulo, color = p.texto2); Spacer(Modifier.height(5.dp)); Text(when(vista){VistaOperacion.PLANIFICAR->"Planificar reparto";VistaOperacion.RUTAS->"Rutas de la empresa";VistaOperacion.CONTROL->"Control del agua";VistaOperacion.AUDITORIA->"Bitácora operativa"}, style = EstilosAgua.TituloParada, color = p.texto); Text(when(vista){VistaOperacion.PLANIFICAR->"Asigna la demanda a una cisterna";VistaOperacion.RUTAS->"Consulta el estado de cada reparto";VistaOperacion.CONTROL->"Autoriza recargas y resuelve diferencias";VistaOperacion.AUDITORIA->"Rastrea quién hizo qué y cuándo"}, style = EstilosAgua.Cuerpo, color = p.texto2) }
        item { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(RadiosAgua.Aviso)).background(p.tenue).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) { TabOperacion("Plan", vista == VistaOperacion.PLANIFICAR, Modifier.weight(1f)) { vista = VistaOperacion.PLANIFICAR }; TabOperacion("Rutas", vista == VistaOperacion.RUTAS, Modifier.weight(1f)) { vista = VistaOperacion.RUTAS; vm.refrescarRutas() }; TabOperacion("Control", vista == VistaOperacion.CONTROL, Modifier.weight(1f)) { vista = VistaOperacion.CONTROL; vm.cargarCustodia() }; TabOperacion("Bitácora", vista == VistaOperacion.AUDITORIA, Modifier.weight(1f)) { vista = VistaOperacion.AUDITORIA; vm.cargarAuditoria() } } }
        when(vista){VistaOperacion.PLANIFICAR->contenidoPlanificar(e,vm);VistaOperacion.RUTAS->contenidoRutas(e,vm,onCancelar={cancelarRuta=it});VistaOperacion.CONTROL->contenidoControl(e,{r,a->accionRecarga=r to a},{resolverIncidencia=it},vm::filtrarRecepciones);VistaOperacion.AUDITORIA->contenidoAuditoria(e,vm)}
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.contenidoPlanificar(e: OperacionUiState, vm: OperacionViewModel) {
    item { ResumenCapacidad(vm.litrosSeleccionados(), e.capacidad, e.seleccionados.size) }
    e.resumen?.let { r ->
        item { SelectorHorizontal("Fecha", listOf(LocalDate.now(), LocalDate.now().plusDays(1)), e.fecha, { it }, { if (it == LocalDate.now()) "Hoy" else "Mañana" }, vm::elegirFecha) }
        item { SelectorHorizontal("Asociación", r.asociaciones, e.asociacionId, { it.id }, { it.nombre }, vm::elegirAsociacion) }
        item { SelectorHorizontal("Cisterna", r.vehiculos, e.vehiculoId, { it.id }, { "${it.placa} · ${it.capacidadLitros} L" }, vm::elegirVehiculo, { it.disponible }, { it.motivoNoDisponible }) }
        item { SelectorHorizontal("Aguatero", r.aguateros, e.aguateroId, { it.id }, { it.nombre }, vm::elegirAguatero, { it.disponible }, { it.motivoNoDisponible }) }
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

private fun androidx.compose.foundation.lazy.LazyListScope.contenidoControl(
    e:OperacionUiState,onAccion:(SolicitudRecarga,String)->Unit,onResolver:(IncidenciaOperativa)->Unit,
    onFiltroRecepcion:(Int?)->Unit,
) {
    item { val p=LocalPaletaAgua.current;Text("SOLICITUDES DE RECARGA",style=EstilosAgua.Rotulo,color=p.texto2) }
    if(e.recargas.isEmpty()) item { MensajeOperacion("No hay solicitudes de recarga") }
    items(e.recargas,key={"recarga-${it.id}"}) { r -> TarjetaRecargaOperador(r,onAccion) }
    item { val p=LocalPaletaAgua.current;Spacer(Modifier.height(8.dp));Text("INCIDENCIAS ABIERTAS",style=EstilosAgua.Rotulo,color=p.texto2) }
    val abiertas=e.incidencias.filter{it.estado in 1..2}
    if(abiertas.isEmpty()) item { MensajeOperacion("No hay diferencias pendientes") }
    items(abiertas,key={"inc-${it.id}"}) { i -> TarjetaIncidencia(i){onResolver(i)} }
    item { val p=LocalPaletaAgua.current;Spacer(Modifier.height(8.dp));Text("RECEPCIONES REGISTRADAS",style=EstilosAgua.Rotulo,color=p.texto2);
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(selected=e.filtroRecepcion==null,onClick={onFiltroRecepcion(null)},label={Text("Todas")});FilterChip(selected=e.filtroRecepcion==2,onClick={onFiltroRecepcion(2)},label={Text("Conformes")});FilterChip(selected=e.filtroRecepcion==3,onClick={onFiltroRecepcion(3)},label={Text("Con diferencia")})}}
    if(e.recepciones.isEmpty()) item { MensajeOperacion("No hay recepciones registradas") }
    items(e.recepciones,key={"recepcion-${it.id}"}) { r -> TarjetaRecepcionOperador(r) }
    e.mensaje?.let { item { MensajeOperacion(it) } }
}

@Composable private fun TarjetaRecepcionOperador(r:RecepcionAgua){val p=LocalPaletaAgua.current;val diferencia=r.estado==3
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(RadiosAgua.Aviso),color=if(diferencia)p.error.copy(alpha=.08f) else p.tenue){Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
        Row{Text(if(diferencia)"CON DIFERENCIA" else "CONFORME",style=EstilosAgua.Rotulo,color=if(diferencia)p.error else p.exito,modifier=Modifier.weight(1f));Text("Ruta #${r.rutaId}",style=EstilosAgua.ItemSub,color=p.texto2)}
        Text(r.puntoNombre,style=EstilosAgua.ItemTitulo,color=p.texto);Text("${r.aguateroNombre} · declaró ${formatearLitros(r.litrosDeclarados)} L",style=EstilosAgua.ItemSub,color=p.texto2);Text("Presidente recibió ${formatearLitros(r.litrosRecibidos)} L",style=EstilosAgua.Cuerpo,color=p.texto)
    }}}

@Composable private fun TarjetaRecargaOperador(r:SolicitudRecarga,onAccion:(SolicitudRecarga,String)->Unit){
    val p=LocalPaletaAgua.current
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(RadiosAgua.Aviso),color=p.tenue){Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
        Row{Text(r.estado.name.replace('_',' '),style=EstilosAgua.Rotulo,color=p.accion,modifier=Modifier.weight(1f));Text("${formatearLitros(r.litrosSolicitados)} L",style=EstilosAgua.ItemTitulo,color=p.texto)}
        Text(r.aguateroNombre,style=EstilosAgua.ItemTitulo,color=p.texto);Text(r.fuenteNombre,style=EstilosAgua.ItemSub,color=p.texto2)
        r.distanciaFuenteMetros?.let{Text("Ubicación registrada a ${it.toInt()} m de la fuente",style=EstilosAgua.ItemSub,color=if(it>250)p.alerta else p.texto2)}
        Text(r.motivo,style=EstilosAgua.Cuerpo,color=p.texto2)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            when(r.estado){
                EstadoSolicitudRecarga.SOLICITADA->{TextButton(onClick={onAccion(r,"autorizar")}){Text("Autorizar")};TextButton(onClick={onAccion(r,"rechazar")}){Text("Rechazar",color=p.error)}}
                EstadoSolicitudRecarga.CARGA_REPORTADA->{TextButton(onClick={onAccion(r,"confirmar")}){Text("Confirmar despacho")}}
                EstadoSolicitudRecarga.AUTORIZADA->Text("Esperando que el aguatero reporte la carga",style=EstilosAgua.ItemSub,color=p.alerta)
                else->Unit
            }
        }
    }}
}

@Composable private fun TarjetaIncidencia(i:IncidenciaOperativa,onResolver:()->Unit){val p=LocalPaletaAgua.current;Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(RadiosAgua.Aviso),color=p.error.copy(alpha=.08f)){Column(Modifier.padding(15.dp)){Text(i.tipo.replace('_',' '),style=EstilosAgua.ItemTitulo,color=p.error);Text(i.detalle,style=EstilosAgua.ItemSub,color=p.texto2);TextButton(onClick=onResolver,modifier=Modifier.align(Alignment.End)){Text("Resolver")}}}}

@Composable private fun DialogoControlRecarga(r:SolicitudRecarga,accion:String,onDismiss:()->Unit,onConfirm:(String,String)->Unit){
    var contacto by remember{mutableStateOf(r.contactoAutorizacion.orEmpty())};var valor by remember{mutableStateOf(if(accion=="confirmar")(r.litrosReportados?:r.litrosSolicitados).toString() else "")}
    val titulo=when(accion){"autorizar"->"Registrar primera llamada";"confirmar"->"Confirmar segunda llamada";else->"Rechazar recarga"}
    AlertDialog(onDismissRequest=onDismiss,title={Text(titulo)},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        if(accion!="rechazar") OutlinedTextField(value=contacto,onValueChange={contacto=it.take(100)},label={Text("Persona contactada en la fuente")})
        OutlinedTextField(value=valor,onValueChange={valor=if(accion=="confirmar")it.filter(Char::isDigit).take(6) else it.take(250)},label={Text(if(accion=="confirmar")"Litros confirmados" else if(accion=="rechazar")"Motivo" else "Nota opcional")})
    }},confirmButton={TextButton(enabled=(accion=="rechazar"&&valor.trim().length>=5)||(accion!="rechazar"&&contacto.isNotBlank()&&(accion!="confirmar"||(valor.toIntOrNull()?:0)>0)),onClick={onConfirm(contacto,valor)}){Text("Confirmar")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}

@Composable private fun DialogoResolverIncidencia(i:IncidenciaOperativa,onDismiss:()->Unit,onConfirm:(String)->Unit){var texto by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Resolver ${i.tipo.replace('_',' ')}")},text={OutlinedTextField(value=texto,onValueChange={texto=it.take(500)},label={Text("Resultado de la investigación")},minLines=3)},confirmButton={TextButton(enabled=texto.trim().length>=10,onClick={onConfirm(texto.trim())}){Text("Cerrar incidencia")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})}

private fun androidx.compose.foundation.lazy.LazyListScope.contenidoAuditoria(e: OperacionUiState, vm: OperacionViewModel) {
    item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { val p=LocalPaletaAgua.current; Text("ÚLTIMOS MOVIMIENTOS",style=EstilosAgua.Rotulo,color=p.texto2,modifier=Modifier.weight(1f));TextButton(onClick=vm::cargarAuditoria){Text("Actualizar",color=p.accion)} } }
    if(e.eventos.isEmpty()) item { val p=LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(RadiosAgua.Aviso),color=p.tenue){Text("Todavía no hay movimientos registrados.",Modifier.padding(18.dp),color=p.texto2)} }
    items(e.eventos,key={"audit-${it.id}"}) { EventoOperacionCard(it) }
    e.mensaje?.let { item { MensajeOperacion(it) } }
}

@Composable private fun EventoOperacionCard(evento:EventoOperacion){val p=LocalPaletaAgua.current;val fecha=java.time.Instant.ofEpochMilli(evento.ocurridoEn).atZone(java.time.ZoneId.systemDefault());Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(RadiosAgua.Aviso),color=p.tenue){Column(Modifier.padding(14.dp)){Row{Text(evento.categoria,style=EstilosAgua.Rotulo,color=p.accion,modifier=Modifier.weight(1f));Text("${fecha.toLocalDate()} ${fecha.toLocalTime().withSecond(0).withNano(0)}",style=EstilosAgua.ItemSub,color=p.texto2)};Text(evento.accion.replace('_',' '),style=EstilosAgua.ItemTitulo,color=p.texto);Text("${evento.actorNombre ?: "Sistema"} · ref. ${evento.referencia}",style=EstilosAgua.ItemSub,color=p.texto2)}}}

@Composable private fun TabOperacion(texto: String, elegido: Boolean, modifier: Modifier, onClick: () -> Unit) { val p = LocalPaletaAgua.current; Surface(modifier.heightIn(min = 46.dp).clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), color = if (elegido) p.tinta else p.tenue, contentColor = if (elegido) p.sobreTinta else p.texto2) { Box(contentAlignment = Alignment.Center) { Text(texto, style = EstilosAgua.ItemTitulo) } } }

@Composable private fun ResumenCapacidad(litros: Int, capacidad: Int, pedidos: Int) { val p = LocalPaletaAgua.current; val proporcion = if (capacidad > 0) (litros.toFloat() / capacidad).coerceIn(0f, 1f) else 0f; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Hoja), color = p.tinta) { Column(Modifier.padding(18.dp)) { Text("DEMANDA SELECCIONADA", style = EstilosAgua.Rotulo, color = p.sobreTinta.copy(alpha = .7f)); Spacer(Modifier.height(6.dp)); Text("${formatearLitros(litros)} de ${formatearLitros(capacidad)} L", style = EstilosAgua.DatoResumen, color = p.sobreTinta); Text("$pedidos pedidos · quedan ${formatearLitros((capacidad-litros).coerceAtLeast(0))} L", style = EstilosAgua.ItemSub, color = p.sobreTinta.copy(alpha = .75f)); Spacer(Modifier.height(12.dp)); Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(p.sobreTinta.copy(alpha = .2f))) { Box(Modifier.fillMaxWidth(proporcion).fillMaxHeight().background(p.accion)) } } } }

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun <T, K> SelectorHorizontal(titulo: String, opciones: List<T>, elegido: K?, id: (T) -> K, texto: (T) -> String, elegir: (K) -> Unit, disponible: (T) -> Boolean = { true }, motivo: (T) -> String? = { null }) { val p = LocalPaletaAgua.current; Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(titulo, style = EstilosAgua.ItemTitulo, color = p.texto); FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { opciones.forEach { opcion -> FilterChip(selected = id(opcion) == elegido, enabled = disponible(opcion), onClick = { elegir(id(opcion)) }, label = { Text(texto(opcion), maxLines = 1) }, colors = FilterChipDefaults.filterChipColors(containerColor = p.hoja, labelColor = p.texto2, selectedContainerColor = p.superficie2, selectedLabelColor = p.texto)) } }; opciones.filterNot(disponible).mapNotNull(motivo).distinct().forEach { Text(it, style = EstilosAgua.ItemSub, color = p.alerta) } } }

@Composable private fun PedidoSeleccionable(pedido: PedidoOperacion, elegido: Boolean, onClick: () -> Unit) { val p = LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(RadiosAgua.Aviso), color = if (elegido) p.tenue else p.hoja) { Row(Modifier.padding(vertical = 10.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(elegido, onCheckedChange = { onClick() }); Column(Modifier.weight(1f)) { Text(pedido.puntoNombre, style = EstilosAgua.ItemTitulo, color = p.texto); Text("Pedido #${pedido.id} · ${pedido.litros} L", style = EstilosAgua.ItemSub, color = p.texto2) } } } }

@Composable private fun RutaOperacionCard(ruta: RutaOperacion, onCancelar: (Long) -> Unit) { val p = LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Aviso), color = p.tenue) { Column(Modifier.padding(16.dp)) { Row { Column(Modifier.weight(1f)) { Text("Ruta #${ruta.id}", style = EstilosAgua.ItemTitulo, color = p.texto); Text(ruta.asociacion.nombre, style = EstilosAgua.Cuerpo, color = p.texto2) }; SuggestionChip(onClick = {}, label = { Text(ruta.estado.etiqueta()) }) }; Spacer(Modifier.height(7.dp)); Text("${ruta.vehiculo.placa} · ${ruta.aguatero.nombre}", style = EstilosAgua.ItemTitulo, color = p.texto); Text("${ruta.fecha} · ${ruta.litros} L · ${ruta.paradas} paradas", style = EstilosAgua.ItemSub, color = p.texto2); if (ruta.estado == EstadoRuta.PLANIFICADA) TextButton(onClick = { onCancelar(ruta.id) }, modifier = Modifier.align(Alignment.End)) { Text("Cancelar ruta", color = p.error) } } } }

@Composable private fun MensajeOperacion(mensaje: String) { val p = LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Aviso), color = if (mensaje.startsWith("Ruta")) p.exito.copy(alpha = .12f) else p.alerta.copy(alpha = .12f)) { Text(mensaje, Modifier.padding(12.dp), style = EstilosAgua.Error, color = if (mensaje.startsWith("Ruta")) p.exito else p.alerta) } }

@Composable private fun ConfirmarPlanificacion(e: OperacionUiState, vm: OperacionViewModel) { val r = e.resumen; val a = r?.asociaciones?.find { it.id == e.asociacionId }; val v = r?.vehiculos?.find { it.id == e.vehiculoId }; val u = r?.aguateros?.find { it.id == e.aguateroId }; AlertDialog(onDismissRequest = vm::cerrarConfirmacion, title = { Text("Revisar planificación") }, text = { Text("${a?.nombre}\n${v?.placa} · ${v?.capacidadLitros} L\n${u?.nombre} · ${e.fecha}\n${e.seleccionados.size} pedidos · ${vm.litrosSeleccionados()} L") }, confirmButton = { TextButton(onClick = vm::planificar) { Text("Crear ruta") } }, dismissButton = { TextButton(onClick = vm::cerrarConfirmacion) { Text("Volver") } }) }

private fun EstadoRuta.etiqueta() = when(this) { EstadoRuta.ABIERTA -> "Abierta"; EstadoRuta.CERRADA -> "Cerrada"; EstadoRuta.PLANIFICADA -> "Planificada"; EstadoRuta.CANCELADA -> "Cancelada" }
