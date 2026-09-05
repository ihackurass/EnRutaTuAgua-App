package pe.enrutatuagua.app.ui.presidente

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronRight
import pe.enrutatuagua.app.R
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.*
import pe.enrutatuagua.domain.modelo.EstadoPedido
import pe.enrutatuagua.domain.modelo.Pedido

@Composable
fun MisPedidosScreen(
    onNuevoPedido: () -> Unit,
    vm: MisPedidosViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsState()
    val error by vm.error.collectAsState()
    val paleta = LocalPaletaAgua.current
    var cancelar by remember { mutableStateOf<Pedido?>(null) }
    val activo = estado.pedidos.firstOrNull { it.estado !in setOf(EstadoPedido.ATENDIDO, EstadoPedido.CANCELADO) }

    cancelar?.let { pedido ->
        AlertDialog(
            onDismissRequest = { cancelar = null },
            title = { Text("Cancelar pedido") },
            text = { Text("El pedido #${pedido.id} dejará de considerarse para las próximas rutas.") },
            confirmButton = {
                TextButton(onClick = { vm.cancelar(pedido.id); cancelar = null }) {
                    Text("Sí, cancelar", color = paleta.error)
                }
            },
            dismissButton = { TextButton(onClick = { cancelar = null }) { Text("Conservar pedido") } },
        )
    }
    LazyColumn(
        Modifier.fillMaxSize().background(paleta.fondo).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { CabeceraPresidente() }
        item { ResumenPresidente(activo) }
        item {
            BotonPrimario(
                texto = "Nuevo pedido",
                onClick = onNuevoPedido,
                iconoPainter = painterResource(R.drawable.ic_presidente_agregar_agua),
            )
        }
        item { RotuloPedidos() }
        when {
            estado.cargando -> item {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = paleta.accion)
                }
            }
            estado.pedidos.isEmpty() -> item { EstadoVacioPresidente() }
            else -> items(estado.pedidos, key = { it.id }) { pedido ->
                PedidoPresidente(pedido, onCancelar = { cancelar = pedido })
            }
        }
        error?.let { item { TarjetaError(it) } }
    }
}

@Composable
private fun CabeceraPresidente() {
    val p = LocalPaletaAgua.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ill_rol_presidente),
            contentDescription = null,
            modifier = Modifier.size(58.dp),
        )
        Spacer(Modifier.width(13.dp))
        Column {
            Text("PRESIDENCIA DE LA ZONA", style = EstilosAgua.Rotulo, color = p.texto2)
            Text(
                "Agua para tu comunidad",
                style = EstilosAgua.TituloParada,
                color = p.texto,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}

@Composable
private fun ResumenPresidente(activo: Pedido?) {
    val p = LocalPaletaAgua.current
    val litros = activo?.items?.sumOf { it.litros }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = p.tinta,
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ABASTECIMIENTO ACTUAL", style = EstilosAgua.Rotulo, color = p.sobreTinta.copy(alpha = .72f))
                Spacer(Modifier.weight(1f))
                activo?.let { EstadoPedidoBadge(it.estado, invertido = true) }
            }
            Spacer(Modifier.height(18.dp))
            Text(litros?.let(::formatearLitros) ?: "—", style = EstilosAgua.CifraHero, color = p.sobreTinta)
            Text(
                if (litros == null) "Sin solicitud activa" else "litros solicitados · Pedido #${activo.id}",
                style = EstilosAgua.Cuerpo,
                color = p.sobreTinta.copy(alpha = .78f),
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = p.sobreTinta.copy(alpha = .16f))
            Spacer(Modifier.height(13.dp))
            Text(
                activo?.let { mensajeEstado(it.estado) }
                    ?: "Cuando la zona necesite agua, crea una solicitud y sigue su avance aquí.",
                style = EstilosAgua.ItemSub,
                color = p.sobreTinta.copy(alpha = .78f),
            )
        }
    }
}

private fun mensajeEstado(estado: EstadoPedido) = when (estado) {
    EstadoPedido.PENDIENTE -> "Esperando que el operador la incluya en una ruta."
    EstadoPedido.RETENIDO -> "La solicitud está retenida y requiere revisión."
    EstadoPedido.PROGRAMADO -> "La solicitud ya fue asignada a una próxima ruta."
    EstadoPedido.EN_RUTA -> "La cisterna ya tiene esta solicitud en su recorrido."
    EstadoPedido.PARCIAL -> "La zona recibió una parte; el saldo sigue pendiente."
    EstadoPedido.ATENDIDO -> "La solicitud fue atendida por completo."
    EstadoPedido.CANCELADO -> "La solicitud fue cancelada."
}

@Composable
private fun RotuloPedidos() {
    val p = LocalPaletaAgua.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "MIS PEDIDOS",
            style = EstilosAgua.Rotulo,
            color = p.texto2,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text("Más reciente primero", style = EstilosAgua.ItemSub, color = p.texto2)
    }
}

@Composable
private fun EstadoVacioPresidente() {
    val p = LocalPaletaAgua.current
    val forma = RoundedCornerShape(12.dp)
    Surface(Modifier.fillMaxWidth().border(1.dp, p.linea, forma), shape = forma, color = p.hoja) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ill_presidente_pedido),
                contentDescription = null,
                modifier = Modifier.size(58.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
            Text("Todavía no hay solicitudes", style = EstilosAgua.ItemTitulo, color = p.texto)
            Spacer(Modifier.height(4.dp))
            Text("La primera solicitud aparecerá aquí con su estado.", style = EstilosAgua.Cuerpo, color = p.texto2)
            }
        }
    }
}

@Composable
private fun PedidoPresidente(pedido: Pedido, onCancelar: () -> Unit) {
    val p = LocalPaletaAgua.current
    val cancelable = pedido.estado == EstadoPedido.PENDIENTE || pedido.estado == EstadoPedido.RETENIDO
    val forma = RoundedCornerShape(12.dp)
    Surface(
        Modifier.fillMaxWidth().border(1.dp, p.linea, forma),
        shape = forma,
        color = p.hoja,
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Image(
                    painter = painterResource(iconoEstado(pedido.estado)),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pedido #${pedido.id}", style = EstilosAgua.ItemSub, color = p.texto2)
                    Text(
                        "${formatearLitros(pedido.items.sumOf { it.litros })} L",
                        style = EstilosAgua.DatoResumen,
                        color = p.texto,
                    )
                }
                EstadoPedidoBadge(pedido.estado)
            }
            Spacer(Modifier.height(10.dp))
            Text(mensajeEstado(pedido.estado), style = EstilosAgua.ItemSub, color = p.texto2)
            if (cancelable) {
                TextButton(onClick = onCancelar, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                    Text("Cancelar pedido", color = p.error)
                }
            }
        }
    }
}

private fun iconoEstado(estado: EstadoPedido) = when (estado) {
    EstadoPedido.PENDIENTE, EstadoPedido.RETENIDO -> R.drawable.ic_presidente_pendiente
    EstadoPedido.PROGRAMADO, EstadoPedido.EN_RUTA, EstadoPedido.PARCIAL -> R.drawable.ill_presidente_asignado
    EstadoPedido.ATENDIDO -> R.drawable.ic_presidente_atendido
    EstadoPedido.CANCELADO -> R.drawable.ic_presidente_cancelado
}

@Composable
private fun EstadoPedidoBadge(estado: EstadoPedido, invertido: Boolean = false) {
    val p = LocalPaletaAgua.current
    val fondo = if (invertido) p.sobreTinta else when (estado) {
        EstadoPedido.ATENDIDO -> p.exito
        EstadoPedido.EN_RUTA, EstadoPedido.PARCIAL -> p.accion
        EstadoPedido.CANCELADO, EstadoPedido.RETENIDO -> p.alerta
        else -> p.superficie2
    }
    val texto = if (invertido) p.tinta else when (estado) {
        EstadoPedido.ATENDIDO, EstadoPedido.EN_RUTA, EstadoPedido.PARCIAL,
        EstadoPedido.CANCELADO, EstadoPedido.RETENIDO ->
            if (p.esClara) Color.White else Color(0xFF101820)
        else -> p.texto2
    }
    Text(
        estado.etiqueta,
        style = EstilosAgua.ChipEstado,
        color = texto,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = 11.dp, vertical = 7.dp),
    )
}

@Composable
private fun GestionZona(onClick: () -> Unit) {
    val p = LocalPaletaAgua.current
    Column {
        Text(
            "GESTIÓN DE LA ZONA",
            style = EstilosAgua.Rotulo,
            color = p.texto2,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(9.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.hoja)
                .border(1.dp, p.linea, RoundedCornerShape(12.dp)).clickable(onClick = onClick)
                .padding(horizontal = 17.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ill_presidente_puntos),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Puntos de abastecimiento", style = EstilosAgua.ItemTitulo, color = p.texto)
                Text("Revisa y valida los lugares de entrega", style = EstilosAgua.ItemSub, color = p.texto2)
            }
            Icon(TablerIcons.ChevronRight, "Abrir puntos de abastecimiento", tint = p.texto2)
        }
    }
}
