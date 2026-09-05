package pe.enrutatuagua.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import compose.icons.TablerIcons
import compose.icons.tablericons.CircleCheck
import compose.icons.tablericons.AlertTriangle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
    var esError by mutableStateOf(false)
        private set

    fun mostrar(texto: String, error: Boolean = false) {
        mensaje = texto
        esError = error
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
        enter = scaleIn(tween(MovimientoAgua.Aviso, easing = MovimientoAgua.EasingSalida), initialScale = .82f) +
            fadeIn(tween(MovimientoAgua.Aviso)),
        exit = scaleOut(tween(MovimientoAgua.Aviso), targetScale = .9f) + fadeOut(tween(MovimientoAgua.Aviso)),
    ) {
        var textoMostrado by remember { mutableStateOf("") }
        estado.mensaje?.let { textoMostrado = it }
        Surface(
            modifier = Modifier.widthIn(min = 176.dp, max = 280.dp),
            shape = RoundedCornerShape(RadiosAgua.Tarjeta),
            color = paleta.hoja,
            shadowElevation = 12.dp,
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("aviso"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(shape = RoundedCornerShape(18.dp), color = if (estado.esError) paleta.error else paleta.exito) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            if (estado.esError) TablerIcons.AlertTriangle else TablerIcons.CircleCheck,
                            contentDescription = null,
                            tint = paleta.hoja,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Text(
                    textoMostrado,
                    modifier = Modifier.padding(top = 11.dp),
                    style = EstilosAgua.ItemTitulo,
                    color = paleta.texto,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}
