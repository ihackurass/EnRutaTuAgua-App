package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    peligro: Boolean = false,
) {
    val esquema = MaterialTheme.colorScheme
    val fondo = if (peligro) esquema.error else esquema.primary
    val contenido = if (peligro) esquema.onError else esquema.onPrimary
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.fillMaxWidth().heightIn(min = DimensionesAgua.AltoBotonPrimario),
        shape = RoundedCornerShape(RadiosAgua.BotonPrimario),
        colors = ButtonDefaults.buttonColors(
            containerColor = fondo,
            contentColor = contenido,
            disabledContainerColor = fondo.copy(alpha = 0.45f),
            disabledContentColor = contenido.copy(alpha = 0.8f),
        ),
    ) {
        Text(texto, style = EstilosAgua.BotonPrimario)
    }
}
