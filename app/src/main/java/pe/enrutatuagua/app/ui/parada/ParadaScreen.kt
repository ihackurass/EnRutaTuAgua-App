package pe.enrutatuagua.app.ui.parada

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.CifraLitros
import pe.enrutatuagua.designsystem.component.DeslizadorConfirmar
import pe.enrutatuagua.designsystem.component.GridMotivos
import pe.enrutatuagua.designsystem.component.RotuloSeccion
import pe.enrutatuagua.designsystem.component.TecladoLitros
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua
import pe.enrutatuagua.domain.modelo.MotivoOmision

/**
 * Detalle de parada (plan 07 T7): entrega con cifra gigante + presets + teclado
 * propio + deslizador, y omisión con grid de motivos. La regla RN-01 vive en el
 * dominio; el `excede` local es solo feedback inmediato en pantalla.
 */
@Composable
fun ParadaScreen(onHecho: () -> Unit, vm: ParadaViewModel = hiltViewModel()) {
    val hecho by vm.hecho.collectAsState()
    val error by vm.error.collectAsState()
    val parada by vm.parada.collectAsState()
    LaunchedEffect(hecho) { if (hecho) onHecho() }

    var enOmision by rememberSaveable { mutableStateOf(vm.iniciaEnOmision) }
    var litros by rememberSaveable { mutableStateOf("") }
    var precargado by rememberSaveable { mutableStateOf(false) }
    var motivoIndice by rememberSaveable { mutableStateOf<Int?>(null) }
    var comentario by rememberSaveable { mutableStateOf("") }

    // Precarga el pedido de la parada una sola vez
    LaunchedEffect(parada) {
        if (!precargado) parada?.let { litros = it.pedidoLitros.toString(); precargado = true }
    }

    val paleta = LocalPaletaAgua.current
    Column(
        Modifier
            .fillMaxSize()
            .background(paleta.hoja)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            "← Volver a la ruta",
            style = EstilosAgua.Cuerpo,
            color = paleta.texto2,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable {
                    if (enOmision && !vm.iniciaEnOmision) enOmision = false else onHecho()
                }
                .padding(vertical = 12.dp),
        )

        if (!enOmision) {
            VistaEntrega(
                titulo = parada?.titulo ?: "…",
                pedido = parada?.pedidoLitros ?: 0,
                restante = parada?.litrosRestantes ?: 0,
                litros = litros,
                errorDominio = error,
                onLitros = { litros = it },
                onConfirmar = { vm.confirmarEntrega(it) },
                onNoPude = { enOmision = true },
            )
        } else {
            VistaOmision(
                titulo = parada?.titulo ?: "…",
                motivoIndice = motivoIndice,
                comentario = comentario,
                errorDominio = error,
                onMotivo = { motivoIndice = it },
                onComentario = { comentario = it },
                onRegistrar = { motivo ->
                    vm.omitir(motivo, comentario.takeIf { it.isNotBlank() })
                },
            )
        }
    }
}

@Composable
private fun TarjetaError(mensaje: String) {
    val paleta = LocalPaletaAgua.current
    val forma = RoundedCornerShape(RadiosAgua.Aviso)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(paleta.alerta.copy(alpha = 0.12f))
            .border(1.dp, paleta.alerta.copy(alpha = 0.4f), forma)
            .padding(12.dp),
    ) {
        Text(mensaje, style = EstilosAgua.Error, color = paleta.alerta)
    }
}

@Composable
private fun VistaEntrega(
    titulo: String,
    pedido: Int,
    restante: Int,
    litros: String,
    errorDominio: String?,
    onLitros: (String) -> Unit,
    onConfirmar: (Int) -> Unit,
    onNoPude: () -> Unit,
) {
    val paleta = LocalPaletaAgua.current
    val valor = litros.toIntOrNull() ?: 0
    val excede = valor > restante
    val invalido = excede || valor <= 0

    RotuloSeccion("Entrega en", acento = titulo)
    Spacer(Modifier.height(10.dp))

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CifraLitros(valor, enAlerta = excede)
        Spacer(Modifier.height(4.dp))
        Text(
            "pidió ${formatearLitros(pedido)} L · quedan ${formatearLitros(restante)} L",
            style = EstilosAgua.Cuerpo, color = paleta.texto2,
        )
    }
    Spacer(Modifier.height(12.dp))

    val mensajeError = errorDominio
        ?: if (excede) "Quedan ${formatearLitros(restante)} L en el tanque. Baja la cantidad o recarga antes de entregar." else null
    mensajeError?.let {
        TarjetaError(it)
        Spacer(Modifier.height(12.dp))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1000, 2000, pedido).distinct().forEach { preset ->
            val forma = RoundedCornerShape(RadiosAgua.Preset)
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(forma)
                    .border(1.dp, paleta.linea, forma)
                    .clickable { onLitros(preset.toString()) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("${formatearLitros(preset)} L", style = EstilosAgua.Preset, color = paleta.texto)
            }
        }
    }
    Spacer(Modifier.height(14.dp))

    TecladoLitros(valor = litros, onCambio = onLitros)
    Spacer(Modifier.height(16.dp))

    DeslizadorConfirmar(
        texto = if (invalido) "Corrige la cantidad" else "Desliza para confirmar ${formatearLitros(valor)} L",
        alerta = invalido,
        onConfirmar = { if (!invalido) onConfirmar(valor) },
    )
    Spacer(Modifier.height(10.dp))

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            "No pude entregar",
            style = EstilosAgua.ItemTitulo,
            color = paleta.texto2,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(onClick = onNoPude)
                .padding(12.dp),
        )
    }
}

@Composable
private fun VistaOmision(
    titulo: String,
    motivoIndice: Int?,
    comentario: String,
    errorDominio: String?,
    onMotivo: (Int) -> Unit,
    onComentario: (String) -> Unit,
    onRegistrar: (MotivoOmision) -> Unit,
) {
    val paleta = LocalPaletaAgua.current
    val motivos = MotivoOmision.entries
    val motivo = motivoIndice?.let { motivos[it] }
    val faltaComentario = motivo == MotivoOmision.OTRO && comentario.isBlank()

    RotuloSeccion("Omitir", acento = titulo)
    Spacer(Modifier.height(6.dp))
    Text("¿Qué pasó?", style = EstilosAgua.TituloParada, color = paleta.texto)
    Spacer(Modifier.height(4.dp))
    Text(
        "La parada queda registrada como no entregada, con su motivo.",
        style = EstilosAgua.Cuerpo, color = paleta.texto2,
    )
    Spacer(Modifier.height(14.dp))

    GridMotivos(
        motivos = motivos.map { it.etiqueta },
        seleccionado = motivoIndice,
        onSeleccionar = onMotivo,
    )
    Spacer(Modifier.height(12.dp))

    if (motivo == MotivoOmision.OTRO) {
        val forma = RoundedCornerShape(RadiosAgua.Comentario)
        BasicTextField(
            value = comentario,
            onValueChange = onComentario,
            textStyle = EstilosAgua.Cuerpo.copy(color = paleta.texto),
            cursorBrush = SolidColor(paleta.accion),
            decorationBox = { interior ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 84.dp)
                        .clip(forma)
                        .background(paleta.tenue)
                        .border(1.dp, paleta.linea, forma)
                        .padding(12.dp),
                ) {
                    if (comentario.isEmpty()) {
                        Text("Cuenta qué pasó, en una línea", style = EstilosAgua.Cuerpo, color = paleta.texto2)
                    }
                    interior()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }

    val mensajeError = errorDominio
        ?: if (faltaComentario) "Escribe qué pasó para poder registrar la omisión." else null
    mensajeError?.let {
        TarjetaError(it)
        Spacer(Modifier.height(12.dp))
    }

    BotonPrimario(
        texto = "Registrar omisión",
        peligro = true,
        habilitado = motivo != null && !faltaComentario,
        onClick = { motivo?.let(onRegistrar) },
    )
    Spacer(Modifier.height(24.dp))
}
