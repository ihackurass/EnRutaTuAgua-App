package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

/** Chip de estado sólido (teal): el único acento saturado de la cabecera. */
@Composable
fun ChipEstadoRuta(texto: String, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    Row(
        modifier
            .clip(RoundedCornerShape(RadiosAgua.Chip))
            .background(paleta.accion)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f)),
        )
        Spacer(Modifier.width(5.dp))
        Text(texto.uppercase(), style = EstilosAgua.ChipEstado, color = Color.White)
    }
}
