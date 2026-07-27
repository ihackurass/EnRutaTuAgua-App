package pe.enrutatuagua.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.MovimientoAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

/**
 * Elemento firma: tubo vertical que hace visible cuánta agua queda en el tanque.
 * La fracción baja animada (700ms, curva firma) con cada entrega — RN-01 se ve
 * antes de romperse.
 */
@Composable
fun ColumnaDeAgua(litrosRestantes: Int, capacidadLitros: Int, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    val fraccion = (litrosRestantes.toFloat() / capacidadLitros.coerceAtLeast(1)).coerceIn(0f, 1f)
    val fraccionAnimada by animateFloatAsState(
        targetValue = fraccion,
        animationSpec = tween(MovimientoAgua.Tubo, easing = MovimientoAgua.EasingSalida),
        label = "tubo",
    )
    val formaTubo = RoundedCornerShape(RadiosAgua.Tubo)

    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatearLitros(litrosRestantes), style = EstilosAgua.ColumnaCifra, color = paleta.texto)
            Text("L", style = EstilosAgua.PieColumna, color = paleta.texto2,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
        }
        Box(
            Modifier
                .size(DimensionesAgua.AnchoTubo, DimensionesAgua.AltoTubo)
                .clip(formaTubo)
                .background(paleta.hoja.copy(alpha = 0.9f))
                .border(1.dp, paleta.linea, formaTubo)
                .testTag("tubo"),
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(fraccionAnimada)
                    .background(paleta.accion)
                    .testTag("tubo-agua"),
            )
            listOf(0.25f, 0.5f, 0.75f).forEach { marca ->
                Box(
                    Modifier
                        .offset(y = DimensionesAgua.AltoTubo * marca)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(paleta.texto.copy(alpha = 0.16f)),
                )
            }
        }
        Text("de ${formatearLitros(capacidadLitros)} L", style = EstilosAgua.PieColumna, color = paleta.texto2)
    }
}
