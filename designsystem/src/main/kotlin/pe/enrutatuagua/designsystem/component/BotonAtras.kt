package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

/**
 * Botón de volver circular, patrón nativo (Uber/Rappi/Yango) en vez de un link
 * de texto: 48dp, fondo `tenue`, icono de un solo trazo. Zona táctil completa.
 */
@Composable
fun BotonAtras(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(paleta.tenue)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Volver" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(TablerIcons.ArrowLeft, contentDescription = null, tint = paleta.texto,
            modifier = Modifier.size(22.dp))
    }
}
