package pe.enrutatuagua.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.MovimientoAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

/** Estado del aviso superior: mostrar() lo enseña 2600ms y se oculta solo. */
class EstadoAviso internal constructor() {
    var mensaje by mutableStateOf<String?>(null)
        private set
    internal var generacion by mutableIntStateOf(0)
        private set

    fun mostrar(texto: String) {
        mensaje = texto
        generacion++
    }

    internal fun ocultar() {
        mensaje = null
    }
}

@Composable
fun rememberEstadoAviso(): EstadoAviso = remember { EstadoAviso() }

@Composable
fun AvisoSuperior(estado: EstadoAviso, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current

    LaunchedEffect(estado.generacion) {
        if (estado.mensaje != null) {
            delay(MovimientoAgua.AvisoVisible)
            estado.ocultar()
        }
    }

    AnimatedVisibility(
        visible = estado.mensaje != null,
        modifier = modifier,
        enter = slideInVertically(tween(MovimientoAgua.Aviso, easing = MovimientoAgua.EasingSalida)) { -it * 2 } + fadeIn(tween(MovimientoAgua.Aviso)),
        exit = slideOutVertically(tween(MovimientoAgua.Aviso)) { -it * 2 } + fadeOut(tween(MovimientoAgua.Aviso)),
    ) {
        var textoMostrado by remember { mutableStateOf("") }
        estado.mensaje?.let { textoMostrado = it }
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiosAgua.Aviso))
                    .background(paleta.texto)
                    .padding(horizontal = 15.dp, vertical = 13.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("aviso"),
            ) {
                Text(textoMostrado, style = EstilosAgua.ItemTitulo, color = paleta.hoja)
            }
        }
    }
}
