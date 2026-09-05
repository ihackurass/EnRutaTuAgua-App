package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

/** Motivo con su pictograma: la celda se reconoce sin leer. */
data class MotivoUi(val etiqueta: String, val icono: ImageVector)

/**
 * Grid 2 columnas de motivos; celdas rellenas `superficie2` ≥56dp. La selección
 * es neutra (borde `tinta`), no una alarma: elegir un motivo no es el error.
 */
@Composable
fun GridMotivos(
    motivos: List<MotivoUi>,
    seleccionado: Int?,
    onSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paleta = LocalPaletaAgua.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DimensionesAgua.GapMotivos)) {
        motivos.chunked(2).forEachIndexed { fila, par ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DimensionesAgua.GapMotivos)) {
                par.forEachIndexed { col, motivo ->
                    val indice = fila * 2 + col
                    val sel = seleccionado == indice
                    val forma = RoundedCornerShape(RadiosAgua.Motivo)
                    val colorContenido = if (sel) paleta.tinta else paleta.texto
                    Row(
                        Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(forma)
                            .background(paleta.superficie2)
                            .let { if (sel) it.border(2.dp, paleta.tinta, forma) else it }
                            .clickable { onSeleccionar(indice) }
                            .padding(horizontal = 12.dp)
                            .testTag("motivo-$indice"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            motivo.icono,
                            contentDescription = null,
                            tint = if (sel) paleta.tinta else paleta.texto2,
                            modifier = Modifier.size(DimensionesAgua.IconoLinea),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(motivo.etiqueta, style = EstilosAgua.ItemTitulo, color = colorContenido)
                    }
                }
                if (par.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
