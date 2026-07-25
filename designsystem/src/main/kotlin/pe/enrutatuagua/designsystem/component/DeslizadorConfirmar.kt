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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
    val color = if (alerta) paleta.alerta else paleta.accion
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

    LaunchedEffect(estado) {
        snapshotFlow { estado.settledValue }.collect { valor ->
            if (valor == ExtremoDeslizador.Fin) {
                haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                onConfirmar()
                estado.animateTo(ExtremoDeslizador.Inicio)
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(DimensionesAgua.AltoDeslizador)
            .clip(formaPista)
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.32f), formaPista)
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
            color = color,
            modifier = Modifier.align(Alignment.Center).padding(start = 46.dp),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = DimensionesAgua.MargenPerilla)
                .offset { IntOffset(estado.requireOffset().roundToInt(), 0) }
                .size(DimensionesAgua.AnchoPerilla, DimensionesAgua.AltoPerilla)
                .clip(RoundedCornerShape(RadiosAgua.Perilla))
                .background(color)
                .anchoredDraggable(estado, Orientation.Horizontal)
                .testTag("perilla"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = texto,
                tint = paleta.hoja,
                modifier = Modifier.size(DimensionesAgua.IconoPerilla),
            )
        }
    }
}
