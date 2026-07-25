package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
fun TarjetaDato(valor: String, rotulo: String, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    Column(
        modifier
            .clip(RoundedCornerShape(RadiosAgua.Tarjeta))
            .background(paleta.tenue)
            .padding(14.dp),
    ) {
        Text(valor, style = EstilosAgua.DatoResumen, color = paleta.texto)
        Text(rotulo.uppercase(), style = EstilosAgua.Rotulo, color = paleta.texto2)
    }
}
