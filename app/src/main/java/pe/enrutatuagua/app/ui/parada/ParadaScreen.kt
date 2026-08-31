package pe.enrutatuagua.app.ui.parada

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.TablerIcons
import compose.icons.tablericons.Ban
import compose.icons.tablericons.Dots
import compose.icons.tablericons.Droplet
import compose.icons.tablericons.DropletOff
import compose.icons.tablericons.UserOff
import pe.enrutatuagua.app.R
import pe.enrutatuagua.designsystem.component.BotonAtras
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.CifraLitros
import pe.enrutatuagua.designsystem.component.DeslizadorConfirmar
import pe.enrutatuagua.designsystem.component.GridMotivos
import pe.enrutatuagua.designsystem.component.MotivoUi
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
fun ParadaScreen(onHecho: (mensajeAviso: String?) -> Unit, vm: ParadaViewModel = hiltViewModel()) {
    val hecho by vm.hecho.collectAsState()
    val error by vm.error.collectAsState()
    val parada by vm.parada.collectAsState()

    var enOmision by rememberSaveable { mutableStateOf(vm.iniciaEnOmision) }
    var litros by rememberSaveable { mutableStateOf("") }
    var precargado by rememberSaveable { mutableStateOf(false) }
    var motivoIndice by rememberSaveable { mutableStateOf<Int?>(null) }
    var comentario by rememberSaveable { mutableStateOf("") }
    var mensajeAviso by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(hecho) { if (hecho) onHecho(mensajeAviso) }

    // Precarga el pedido de la parada una sola vez
    LaunchedEffect(parada) {
        if (!precargado) parada?.let { litros = it.pedidoLitros.toString(); precargado = true }
    }

    val paleta = LocalPaletaAgua.current
    Column(
        Modifier
            .fillMaxSize()
            .background(paleta.fondo)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BotonAtras(onClick = {
            if (enOmision && !vm.iniciaEnOmision) enOmision = false else onHecho(null)
        })
        Spacer(Modifier.height(24.dp))

        if (!enOmision) {
            VistaEntrega(
                titulo = parada?.titulo ?: "…",
                pedido = parada?.pedidoLitros ?: 0,
                restante = parada?.litrosRestantes ?: 0,
                litros = litros,
                errorDominio = error,
                onLitros = { litros = it },
                onConfirmar = {
                    mensajeAviso = "Entrega registrada: ${formatearLitros(it)} L"
                    vm.confirmarEntrega(it)
                },
                onNoPude = { enOmision = true },
                modifier = Modifier.weight(1f),
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
                    mensajeAviso = "Parada omitida: ${motivo.etiqueta}"
                    vm.omitir(motivo, comentario.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun iconoDeMotivo(motivo: MotivoOmision): ImageVector = when (motivo) {
    MotivoOmision.CALLE_CERRADA -> TablerIcons.Ban
    MotivoOmision.NADIE_PRESENTE -> TablerIcons.UserOff
    MotivoOmision.FALTA_AGUA -> TablerIcons.DropletOff
    MotivoOmision.OTRO -> TablerIcons.Dots
}

@Composable
private fun TarjetaError(mensaje: String, mostrarSinAgua: Boolean = false) {
    val paleta = LocalPaletaAgua.current
    val forma = RoundedCornerShape(RadiosAgua.Aviso)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(paleta.hoja)
            .border(1.dp, paleta.error, forma)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mostrarSinAgua) {
            Image(
                painter = painterResource(R.drawable.ic_aguatero_sin_agua),
                contentDescription = null,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = mensaje,
            modifier = Modifier.weight(1f),
            style = EstilosAgua.Error,
            color = paleta.error,
        )
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
    modifier: Modifier = Modifier,
) {
    val paleta = LocalPaletaAgua.current
    val valor = litros.toIntOrNull() ?: 0
    val excede = valor > restante
    val invalido = excede || valor <= 0

    // Bloque superior fijo (cifra + error) y bloque de acción ANCLADO abajo
    // (presets+teclado+deslizador): el error vive BAJO la cifra para que el
    // teclado nunca se desplace cuando aparece — solo respira el Spacer(weight).
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ill_aguatero_entrega),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(8.dp))
            RotuloSeccion("Entrega en", modifier = Modifier.weight(1f), acento = titulo)
        }
        Spacer(Modifier.height(10.dp))

        Column(
            Modifier.fillMaxWidth().semantics { heading() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CifraLitros(valor, enAlerta = excede)
            Spacer(Modifier.height(4.dp))
            Text(
                "pidió ${formatearLitros(pedido)} L · quedan ${formatearLitros(restante)} L",
                style = EstilosAgua.Cuerpo, color = paleta.texto2,
            )
        }

        val mensajeError = errorDominio
            ?: if (excede) "Quedan ${formatearLitros(restante)} L en el tanque. Baja la cantidad o recarga antes de entregar." else null
        mensajeError?.let {
            Spacer(Modifier.height(14.dp))
            TarjetaError(it, mostrarSinAgua = restante == 0 || excede)
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1000, 2000, pedido).distinct().forEach { preset ->
                val forma = RoundedCornerShape(RadiosAgua.Preset)
                val elegido = preset.toString() == litros
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(forma)
                        .background(if (elegido) paleta.accion else paleta.hoja)
                        .border(1.dp, if (elegido) paleta.accion else paleta.linea, forma)
                        .clickable { onLitros(preset.toString()) }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        TablerIcons.Droplet,
                        contentDescription = null,
                        tint = if (elegido) paleta.sobreTinta else paleta.texto2,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${formatearLitros(preset)} L",
                        style = EstilosAgua.Preset,
                        color = if (elegido) paleta.sobreTinta else paleta.texto,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))   // presets + teclado: mismo bloque, tacto ajustado

        TecladoLitros(valor = litros, onCambio = onLitros)
        Spacer(Modifier.height(20.dp))

        DeslizadorConfirmar(
            texto = if (invalido) "Corrige la cantidad" else "Desliza para confirmar ${formatearLitros(valor)} L",
            alerta = invalido,
            onConfirmar = { if (!invalido) onConfirmar(valor) },
        )
        Spacer(Modifier.height(12.dp))

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
    modifier: Modifier = Modifier,
) {
    val paleta = LocalPaletaAgua.current
    val motivos = MotivoOmision.entries
    val motivo = motivoIndice?.let { motivos[it] }
    val faltaComentario = motivo == MotivoOmision.OTRO && comentario.isBlank()

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        RotuloSeccion("Omitir", acento = titulo)
        Spacer(Modifier.height(6.dp))
        Text(
            "¿Qué pasó?",
            style = EstilosAgua.TituloParada,
            color = paleta.texto,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "La parada queda registrada como no entregada, con su motivo.",
            style = EstilosAgua.Cuerpo, color = paleta.texto2,
        )
        Spacer(Modifier.height(14.dp))

        GridMotivos(
            motivos = motivos.map { MotivoUi(etiqueta = it.etiqueta, icono = iconoDeMotivo(it)) },
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
                            .background(paleta.hoja)
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
}
