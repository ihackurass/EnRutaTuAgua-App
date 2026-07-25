package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

@Composable
fun ChipEstadoRuta(texto: String, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    val forma = RoundedCornerShape(RadiosAgua.Chip)
    Box(
        modifier
            .clip(forma)
            .background(paleta.accion.copy(alpha = 0.16f))
            .border(1.dp, paleta.accion.copy(alpha = 0.34f), forma)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(texto.uppercase(), style = EstilosAgua.ChipEstado, color = paleta.accion)
    }
}
