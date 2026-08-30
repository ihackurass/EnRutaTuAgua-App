package pe.enrutatuagua.app.ui.presidente

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.*
import pe.enrutatuagua.domain.modelo.EstadoPedido
import pe.enrutatuagua.domain.modelo.Pedido

@Composable
fun MisPedidosScreen(onNuevoPedido: () -> Unit, onValidarPuntos: () -> Unit, vm: MisPedidosViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsState(); val error by vm.error.collectAsState(); val paleta = LocalPaletaAgua.current
    var cancelar by remember { mutableStateOf<Pedido?>(null) }
    val activo = estado.pedidos.firstOrNull { it.estado !in setOf(EstadoPedido.ATENDIDO, EstadoPedido.CANCELADO) }
    cancelar?.let { pedido -> AlertDialog(
        onDismissRequest = { cancelar = null }, title = { Text("Cancelar pedido") },
        text = { Text("El pedido #${pedido.id} dejará de considerarse para las próximas rutas.") },
        confirmButton = { TextButton(onClick = { vm.cancelar(pedido.id); cancelar = null }) { Text("Sí, cancelar", color = paleta.error) } },
        dismissButton = { TextButton(onClick = { cancelar = null }) { Text("Conservar pedido") } },
    ) }
    LazyColumn(
        Modifier.fillMaxSize().background(paleta.hoja).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("TU ASOCIACIÓN", style = EstilosAgua.Rotulo, color = paleta.texto2); Spacer(Modifier.height(5.dp)); Text("Gestión de agua", style = EstilosAgua.TituloParada, color = paleta.texto); Text("Pedidos y puntos de abastecimiento", style = EstilosAgua.Cuerpo, color = paleta.texto2) }
        item { ResumenPresidente(activo) }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("MIS PEDIDOS", style = EstilosAgua.Rotulo, color = paleta.texto2, modifier = Modifier.weight(1f)); Text("Historial", style = EstilosAgua.ItemTitulo, color = paleta.accion) } }
        when {
            estado.cargando -> item { Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = paleta.accion) } }
            estado.pedidos.isEmpty() -> item { EstadoVacioPresidente() }
            else -> items(estado.pedidos, key = { it.id }) { pedido -> PedidoPresidente(pedido, onCancelar = { cancelar = pedido }) }
        }
        error?.let { item { TarjetaError(it) } }
        item { Text("GESTIÓN DE LA ZONA", style = EstilosAgua.Rotulo, color = paleta.texto2); Spacer(Modifier.height(8.dp)); Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onValidarPuntos), shape = RoundedCornerShape(RadiosAgua.Aviso), color = paleta.tenue) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Revisar puntos propuestos", style = EstilosAgua.ItemTitulo, color = paleta.texto); Text("Aprueba lugares que podrán recibir agua", style = EstilosAgua.ItemSub, color = paleta.texto2) }; Text("Revisar", style = EstilosAgua.ItemTitulo, color = paleta.accion) } } }
        item { BotonPrimario("Nuevo pedido", onClick = onNuevoPedido) }
    }
}

@Composable private fun ResumenPresidente(activo: Pedido?) { val p = LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Hoja), color = p.tinta) { Column(Modifier.padding(18.dp)) { Text("ABASTECIMIENTO ACTUAL", style = EstilosAgua.Rotulo, color = p.sobreTinta.copy(alpha = .7f)); Spacer(Modifier.height(7.dp)); Text(activo?.estado?.etiqueta ?: "Sin pedido activo", style = EstilosAgua.DatoResumen, color = p.sobreTinta); Text(activo?.let { "${formatearLitros(it.items.sumOf { item -> item.litros })} L · Pedido #${it.id}" } ?: "Crea un pedido cuando tu zona necesite agua", style = EstilosAgua.Cuerpo, color = p.sobreTinta.copy(alpha = .78f)) } } }

@Composable private fun EstadoVacioPresidente() { val p = LocalPaletaAgua.current; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Aviso), color = p.tenue) { Column(Modifier.padding(18.dp)) { Text("Todavía no tienes pedidos", style = EstilosAgua.ItemTitulo, color = p.texto); Text("Cuando solicites agua, aquí podrás seguir su estado.", style = EstilosAgua.Cuerpo, color = p.texto2) } } }

@Composable private fun PedidoPresidente(pedido: Pedido, onCancelar: () -> Unit) { val p = LocalPaletaAgua.current; val cancelable = pedido.estado == EstadoPedido.PENDIENTE || pedido.estado == EstadoPedido.RETENIDO; Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Aviso), color = p.tenue) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Pedido #${pedido.id}", style = EstilosAgua.ItemTitulo, color = p.texto); Text("${formatearLitros(pedido.items.sumOf { it.litros })} L", style = EstilosAgua.Cuerpo, color = p.texto2) }; SuggestionChip(onClick = {}, label = { Text(pedido.estado.etiqueta) }) }; if (cancelable) TextButton(onClick = onCancelar, modifier = Modifier.align(Alignment.End)) { Text("Cancelar pedido", color = p.error) } } } }
