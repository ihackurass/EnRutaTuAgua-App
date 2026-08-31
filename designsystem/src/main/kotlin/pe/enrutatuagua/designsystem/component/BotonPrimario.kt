package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    peligro: Boolean = false,
    icono: ImageVector? = null,
    iconoPainter: Painter? = null,
) {
    val esquema = MaterialTheme.colorScheme
    val paleta = LocalPaletaAgua.current
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
            disabledContainerColor = paleta.superficie2,
            disabledContentColor = paleta.texto2,
        ),
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(DimensionesAgua.IconoLinea))
            Spacer(Modifier.width(8.dp))
        } else if (iconoPainter != null) {
            Image(
                painter = iconoPainter,
                contentDescription = null,
                modifier = Modifier.size(DimensionesAgua.IconoLinea),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = EstilosAgua.BotonPrimario)
    }
}
