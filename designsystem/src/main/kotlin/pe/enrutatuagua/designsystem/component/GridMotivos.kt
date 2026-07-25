package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

/** Grid 2 columnas de motivos; celdas ≥56dp; seleccionado en alerta (12% + borde). */
@Composable
fun GridMotivos(
    motivos: List<String>,
    seleccionado: Int?,
    onSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paleta = LocalPaletaAgua.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DimensionesAgua.GapMotivos)) {
        motivos.chunked(2).forEachIndexed { fila, par ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DimensionesAgua.GapMotivos)) {
                par.forEachIndexed { col, texto ->
                    val indice = fila * 2 + col
                    val sel = seleccionado == indice
                    val forma = RoundedCornerShape(RadiosAgua.Motivo)
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = DimensionesAgua.AltoTactilMinimo)
                            .clip(forma)
                            .background(if (sel) paleta.alerta.copy(alpha = 0.12f) else Color.Transparent)
                            .border(1.dp, if (sel) paleta.alerta else paleta.linea, forma)
                            .clickable { onSeleccionar(indice) }
                            .padding(horizontal = 12.dp, vertical = 18.dp)
                            .testTag("motivo-$indice"),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(texto, style = EstilosAgua.ItemTitulo,
                            color = if (sel) paleta.alerta else paleta.texto)
                    }
                }
                if (par.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
