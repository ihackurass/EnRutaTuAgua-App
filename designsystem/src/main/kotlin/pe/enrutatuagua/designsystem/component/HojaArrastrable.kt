package pe.enrutatuagua.designsystem.component

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.MovimientoAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

enum class AnclaHoja { Asomo, Abierta }

/**
 * Hoja inferior con dos anclas (asomo 262dp / abierta 86% del alto), esquinas
 * superiores 22dp y asa de 38x4. Se arrastra desde el asa (zona táctil 48dp) o
 * se alterna con un toque. El contenido recibe el ancla para decidir su scroll.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HojaArrastrable(
    modifier: Modifier = Modifier,
    alturaAsomo: Dp = DimensionesAgua.AsomoHoja,
    fraccionAbierta: Float = DimensionesAgua.FraccionHojaAbierta,
    onAncla: (AnclaHoja) -> Unit = {},
    contenido: @Composable (ancla: AnclaHoja) -> Unit,
) {
    val paleta = LocalPaletaAgua.current
    val density = LocalDensity.current
    val alcance = rememberCoroutineScope()
    val onAnclaActual by rememberUpdatedState(onAncla)

    // navigationBarsPadding ACÁ (no sólo en el contenido de adentro): el
    // ancla "Asomo" es una altura fija (262dp) medida contra `maxHeight` --
    // si `maxHeight` incluye la franja de la barra de navegación del
    // sistema (pantalla edge-to-edge), el borde inferior del asomo cae
    // detrás de esa barra sin importar cuánto padding tenga el contenido de
    // adentro (hallazgo real en dispositivo: el botón "No pude entregar"
    // quedaba tapado). Al descontarla acá, TODO lo que se calcule desde
    // `maxHeight` (incluida la posición del ancla) ya excluye esa franja.
    BoxWithConstraints(modifier.fillMaxSize().navigationBarsPadding()) {
        val altoHoja = maxHeight * fraccionAbierta
        val desliz = with(density) { (altoHoja - alturaAsomo).toPx().coerceAtLeast(0f) }

        val estado = remember {
            AnchoredDraggableState(
                initialValue = AnclaHoja.Asomo,
                positionalThreshold = { total -> total * 0.5f },
                velocityThreshold = { with(density) { 800.dp.toPx() } },
                snapAnimationSpec = tween(MovimientoAgua.Hoja, easing = MovimientoAgua.EasingSalida),
                decayAnimationSpec = exponentialDecay(),
            )
        }
        remember(desliz) {
            estado.updateAnchors(
                DraggableAnchors {
                    AnclaHoja.Abierta at 0f
                    AnclaHoja.Asomo at desliz
                },
            )
            desliz
        }
        LaunchedEffect(estado) {
            snapshotFlow { estado.settledValue }.collect { onAnclaActual(it) }
        }

        Surface(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(altoHoja)
                .offset {
                    val y = if (estado.offset.isNaN()) desliz else estado.offset
                    IntOffset(0, y.roundToInt())
                },
            shape = RoundedCornerShape(topStart = RadiosAgua.Hoja, topEnd = RadiosAgua.Hoja),
            color = paleta.hoja,
            shadowElevation = 16.dp,
        ) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(DimensionesAgua.ZonaTactilAsa)
                        .anchoredDraggable(estado, Orientation.Vertical)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            alcance.launch {
                                estado.animateTo(
                                    if (estado.currentValue == AnclaHoja.Abierta) AnclaHoja.Asomo
                                    else AnclaHoja.Abierta,
                                )
                            }
                        }
                        .semantics { contentDescription = "Abrir o cerrar la hoja" }
                        .testTag("asa"),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        Modifier
                            .padding(top = 9.dp)
                            .size(DimensionesAgua.AnchoAsa, DimensionesAgua.AltoAsa)
                            .clip(RoundedCornerShape(RadiosAgua.Asa))
                            .background(paleta.linea),
                    )
                }
                contenido(estado.settledValue)
            }
        }
    }
}
