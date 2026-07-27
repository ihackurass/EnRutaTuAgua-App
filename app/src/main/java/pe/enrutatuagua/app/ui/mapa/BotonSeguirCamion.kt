package pe.enrutatuagua.app.ui.mapa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.CurrentLocation
import compose.icons.tablericons.Route
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

/**
 * FAB del mapa del aguatero: alterna "seguir camión" (cámara detrás del
 * vehículo) ↔ "ver ruta completa" (encuadre de todas las paradas).
 * Sin imports MapLibre a propósito: testeable en Robolectric.
 */
@Composable
fun BotonSeguirCamion(siguiendo: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp).testTag("boton-seguir"),
        shape = CircleShape,
        color = paleta.hoja,
        shadowElevation = 8.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (siguiendo) TablerIcons.Route else TablerIcons.CurrentLocation,
                contentDescription = if (siguiendo) "Ver ruta completa" else "Seguir camión",
                tint = if (siguiendo) paleta.accion else paleta.texto,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
