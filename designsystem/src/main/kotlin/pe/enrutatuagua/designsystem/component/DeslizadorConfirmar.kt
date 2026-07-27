package pe.enrutatuagua.designsystem.component

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowRight
import kotlin.math.roundToInt
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.MovimientoAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

enum class ExtremoDeslizador { Inicio, Fin }

/**
 * Confirmación por gesto (no por toque): la perilla debe recorrer el 82% de la
 * pista. Evita entregas fantasma con guantes o pantalla polvorienta. Háptica al
 * confirmar; la perilla regresa sola (300ms, curva firma).
 * Pista sólida `tinta` (patrón Uber); inválido = gris apagado, no naranja.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeslizadorConfirmar(
    texto: String,
    onConfirmar: () -> Unit,
    modifier: Modifier = Modifier,
    alerta: Boolean = false,
) {
    val paleta = LocalPaletaAgua.current
    val colorPista = if (alerta) paleta.superficie2 else paleta.tinta
    val colorTexto = if (alerta) paleta.texto2 else paleta.sobreTinta
    val colorPerilla = if (alerta) paleta.hoja else paleta.sobreTinta
    val colorIcono = if (alerta) paleta.texto2 else paleta.tinta
    val haptica = LocalHapticFeedback.current
    val density = LocalDensity.current
    val formaPista = RoundedCornerShape(RadiosAgua.Deslizador)

    val estado = remember {
        AnchoredDraggableState(
            initialValue = ExtremoDeslizador.Inicio,
            positionalThreshold = { total -> total * DimensionesAgua.UmbralConfirmacion },
            velocityThreshold = { with(density) { 1000.dp.toPx() } },
            snapAnimationSpec = tween(MovimientoAgua.Perilla, easing = MovimientoAgua.EasingSalida),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    // rememberUpdatedState: el efecto vive una sola vez pero la lambda puede cambiar
    // entre composiciones (p. ej. cuando la cifra pasa de inválida a válida).
    val alConfirmar by rememberUpdatedState(onConfirmar)
    LaunchedEffect(estado) {
        snapshotFlow { estado.settledValue }.collect { valor ->
            if (valor == ExtremoDeslizador.Fin) {
                haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                alConfirmar()
                estado.animateTo(ExtremoDeslizador.Inicio)
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(DimensionesAgua.AltoDeslizador)
            .clip(formaPista)
            .background(colorPista)
            .let { if (alerta) it.border(1.dp, paleta.linea, formaPista) else it }
            .onSizeChanged { medida ->
                val margen = with(density) { DimensionesAgua.MargenPerilla.toPx() }
                val perilla = with(density) { DimensionesAgua.AnchoPerilla.toPx() }
                val tope = (medida.width - perilla - margen * 2).coerceAtLeast(1f)
                estado.updateAnchors(
                    DraggableAnchors {
                        ExtremoDeslizador.Inicio at 0f
                        ExtremoDeslizador.Fin at tope
                    },
                )
            },
    ) {
        Text(
            texto,
            style = EstilosAgua.Deslizador,
            color = colorTexto,
            modifier = Modifier.align(Alignment.Center).padding(start = 46.dp),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = DimensionesAgua.MargenPerilla)
                .offset { IntOffset(estado.requireOffset().roundToInt(), 0) }
                .size(DimensionesAgua.AnchoPerilla, DimensionesAgua.AltoPerilla)
                .clip(RoundedCornerShape(RadiosAgua.Perilla))
                .background(colorPerilla)
                .let { if (alerta) it.border(1.dp, paleta.linea, RoundedCornerShape(RadiosAgua.Perilla)) else it }
                .anchoredDraggable(estado, Orientation.Horizontal)
                .testTag("perilla"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                TablerIcons.ArrowRight,
                contentDescription = texto,
                tint = colorIcono,
                modifier = Modifier.size(DimensionesAgua.IconoPerilla),
            )
        }
    }
}
