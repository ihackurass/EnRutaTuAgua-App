package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

/**
 * Secundario relleno (patrón Uber): bloque gris `superficie2` con texto pleno.
 * Nunca outline fantasma — bajo sol directo un borde de 1dp desaparece.
 */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
) {
    val paleta = LocalPaletaAgua.current
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.fillMaxWidth().heightIn(min = DimensionesAgua.AltoBotonSecundario),
        shape = RoundedCornerShape(RadiosAgua.Secundario),
        colors = ButtonDefaults.buttonColors(
            containerColor = paleta.superficie2,
            contentColor = paleta.texto,
            disabledContainerColor = paleta.superficie2,
            disabledContentColor = paleta.texto2,
        ),
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(DimensionesAgua.IconoLinea))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = EstilosAgua.BotonPrimario)
    }
}
