package pe.enrutatuagua.app.ui.presidente

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.designsystem.component.BotonAtras
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.CifraLitros
import pe.enrutatuagua.designsystem.component.EstadoFicha
import pe.enrutatuagua.designsystem.component.FichaParada
import pe.enrutatuagua.designsystem.component.RotuloSeccion
import pe.enrutatuagua.designsystem.component.TecladoLitros
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.domain.modelo.CondicionesPedido
import pe.enrutatuagua.domain.modelo.PrioridadPedido
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Nuevo pedido (RP-01). Selector de punto validado + cifra/teclado de litros,
 * reutilizados tal cual del flujo de entrega del aguatero (ver `ParadaScreen`):
 * es exactamente el mismo problema de UI, "cuántos litros".
 */
@Composable
fun NuevoPedidoScreen(onHecho: () -> Unit, vm: NuevoPedidoViewModel = hiltViewModel()) {
    val puntos by vm.puntos.collectAsState()
    val hecho by vm.hecho.collectAsState()
    val error by vm.error.collectAsState()
    val limite by vm.limite.collectAsState()
    val items by vm.items.collectAsState()
    LaunchedEffect(hecho) { if (hecho) onHecho() }

    var puntoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var litros by rememberSaveable { mutableStateOf("") }
    var urgente by rememberSaveable { mutableStateOf(false) }
    var motivoUrgente by rememberSaveable { mutableStateOf("") }
    var ventana by rememberSaveable { mutableStateOf("sin") }

    LaunchedEffect(urgente, motivoUrgente, ventana) {
        val ahora = ZonedDateTime.now()
        val rango = when (ventana) {
            "4h" -> ahora.toInstant().toEpochMilli() to ahora.plusHours(4).toInstant().toEpochMilli()
            "manana" -> {
                val inicio = ahora.plusDays(1).toLocalDate().atTime(8, 0).atZone(ZoneId.systemDefault())
                inicio.toInstant().toEpochMilli() to inicio.plusHours(4).toInstant().toEpochMilli()
            }
            else -> null
        }
        vm.configurarCondiciones(CondicionesPedido(
            if (urgente) PrioridadPedido.URGENTE else PrioridadPedido.NORMAL,
            motivoUrgente.takeIf { urgente }, rango?.first, rango?.second,
        ))
    }

    val paleta = LocalPaletaAgua.current
    val valor = litros.toIntOrNull() ?: 0
    val invalido = puntoId == null || valor <= 0 || limite == null || valor > (limite?.maxLitrosPedido ?: 0) ||
        limite?.tienePedidoActivo == true || items.any { it.puntoId == puntoId }

    Column(
        Modifier
            .fillMaxSize()
            .background(paleta.fondo)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BotonAtras(onClick = onHecho)
        Spacer(Modifier.height(24.dp))

        RotuloSeccion("Nuevo pedido")
        Spacer(Modifier.height(10.dp))
        Text(
            "1 · Elige el punto de entrega",
            style = EstilosAgua.ItemTitulo,
            color = paleta.texto,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(6.dp))

        // Un solo scroll permite llegar a la confirmación también en equipos
        // pequeños o con escala de fuente grande.
        puntos.forEach { punto ->
            FichaParada(
                numero = punto.id.toInt(),
                titulo = punto.nombre,
                subtitulo = "${formatearLitros(punto.capacidadLitros)} L de capacidad",
                textoDerecha = if (puntoId == punto.id) "Elegido" else "",
                estado = if (puntoId == punto.id) EstadoFicha.Proxima else EstadoFicha.Pendiente,
                onClick = { puntoId = punto.id; vm.seleccionarPunto(punto.id) },
            )
        }
        limite?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                if (it.tienePedidoActivo) "Este punto ya tiene un pedido activo"
                else "Máximo para este punto: ${formatearLitros(it.maxLitrosPedido)} L. Puede atenderse en varios viajes.",
                style = EstilosAgua.Cuerpo,
                color = if (it.tienePedidoActivo || valor > it.maxLitrosPedido) paleta.error else paleta.texto2,
            )
        }
        Spacer(Modifier.height(20.dp))
        RotuloSeccion("2 · Cantidad de agua")
        Spacer(Modifier.height(8.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CifraLitros(valor)
        }
        Spacer(Modifier.height(16.dp))
        TecladoLitros(valor = litros, onCambio = { litros = it })

        Spacer(Modifier.height(22.dp))
        RotuloSeccion("3 · Condiciones")
        Spacer(Modifier.height(8.dp))
        Text("Horario preferido", style = EstilosAgua.ItemTitulo, color = paleta.texto)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = ventana == "sin", onClick = { ventana = "sin" }, label = { Text("Sin horario") }, colors = coloresFiltroPlano(paleta))
            FilterChip(selected = ventana == "4h", onClick = { ventana = "4h" }, label = { Text("Próximas 4 h") }, colors = coloresFiltroPlano(paleta))
        }
        FilterChip(selected = ventana == "manana", onClick = { ventana = "manana" }, label = { Text("Mañana 8–12") }, colors = coloresFiltroPlano(paleta))
        Spacer(Modifier.height(10.dp))
        FilterChip(selected = urgente, onClick = { urgente = !urgente }, label = { Text("Necesidad urgente") }, colors = coloresFiltroPlano(paleta))
        if (urgente) {
            OutlinedTextField(
                value = motivoUrgente,
                onValueChange = { motivoUrgente = it.take(250) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Motivo (mínimo 10 caracteres)") },
            )
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            TarjetaError(it)
        }
        Spacer(Modifier.height(12.dp))
        BotonPrimario(
            texto = "Agregar punto",
            habilitado = !invalido,
            onClick = { puntoId?.let { id -> vm.agregar(id, valor) } },
        )
        if (items.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            RotuloSeccion("Resumen del pedido", acento = "${items.size} punto${if (items.size == 1) "" else "s"}")
            items.forEach { item ->
                val nombre = puntos.firstOrNull { it.id == item.puntoId }?.nombre ?: "Punto #${item.puntoId}"
                FichaParada(
                    numero = item.puntoId.toInt(), titulo = nombre,
                    subtitulo = "${formatearLitros(item.litros)} L", textoDerecha = "Quitar",
                    estado = EstadoFicha.Proxima, onClick = { vm.quitar(item.puntoId) },
                )
            }
            Spacer(Modifier.height(12.dp))
            BotonPrimario(
                texto = "Enviar pedido · ${items.size} punto${if (items.size == 1) "" else "s"}",
                habilitado = !urgente || motivoUrgente.trim().length >= 10,
                onClick = vm::crearMultipunto,
            )
        }
    }
}

@Composable
private fun coloresFiltroPlano(paleta: pe.enrutatuagua.designsystem.tokens.PaletaAgua) =
    androidx.compose.material3.FilterChipDefaults.filterChipColors(
        containerColor = paleta.hoja,
        labelColor = paleta.texto,
        selectedContainerColor = paleta.accion,
        selectedLabelColor = if (paleta.esClara) Color.White else Color(0xFF101820),
    )
