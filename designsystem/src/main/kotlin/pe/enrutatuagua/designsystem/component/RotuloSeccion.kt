package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

@Composable
fun RotuloSeccion(texto: String, modifier: Modifier = Modifier, acento: String? = null) {
    val paleta = LocalPaletaAgua.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(texto.uppercase(), style = EstilosAgua.Rotulo, color = paleta.texto2)
        acento?.let {
            Text(it.uppercase(), style = EstilosAgua.Rotulo, color = paleta.accion)
        }
    }
}
