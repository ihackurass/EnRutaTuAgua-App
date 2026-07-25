package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

/** Regla del teclado: reemplaza "0" inicial, corta a 6 dígitos. */
internal fun pulsarTecla(valor: String, tecla: String): String = when (tecla) {
    "C" -> ""
    "⌫" -> valor.dropLast(1)
    else -> if (valor.length < 6) (if (valor == "0") "" else valor) + tecla else valor
}

@Composable
fun TecladoLitros(valor: String, onCambio: (String) -> Unit, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    val filas = listOf(
        listOf("1", "2", "3"), listOf("4", "5", "6"),
        listOf("7", "8", "9"), listOf("C", "0", "⌫"),
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DimensionesAgua.GapTeclado)) {
        filas.forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DimensionesAgua.GapTeclado)) {
                fila.forEach { tecla ->
                    val esUtil = tecla == "C" || tecla == "⌫"
                    val tag = when (tecla) {
                        "C" -> "tecla-C"; "⌫" -> "tecla-borrar"; else -> "tecla-$tecla"
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(DimensionesAgua.AltoTecla)
                            .clip(RoundedCornerShape(RadiosAgua.Tecla))
                            .border(1.dp, paleta.linea, RoundedCornerShape(RadiosAgua.Tecla))
                            .clickable { onCambio(pulsarTecla(valor, tecla)) }
                            .testTag(tag),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            tecla,
                            style = if (esUtil) EstilosAgua.Deslizador else EstilosAgua.TeclaDigito,
                            color = if (esUtil) paleta.texto2 else paleta.texto,
                        )
                    }
                }
            }
        }
    }
}
