package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = DimensionesAgua.AltoPreset),
        shape = RoundedCornerShape(RadiosAgua.Secundario),
        border = BorderStroke(1.dp, paleta.linea),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = paleta.texto2),
    ) {
        Text(texto, style = EstilosAgua.ItemTitulo)
    }
}
