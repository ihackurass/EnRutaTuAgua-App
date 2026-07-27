package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import java.text.NumberFormat
import java.util.Locale

/** Formato es-PE con separador de miles: 3500 -> "3,500". */
fun formatearLitros(litros: Int): String =
    NumberFormat.getIntegerInstance(Locale("es", "PE")).format(litros.toLong())

@Composable
fun CifraLitros(litros: Int, modifier: Modifier = Modifier, enAlerta: Boolean = false) {
    val paleta = LocalPaletaAgua.current
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            formatearLitros(litros),
            style = EstilosAgua.CifraHero,
            // Exceder el tanque es un error (RN-01), no una advertencia.
            color = if (enAlerta) paleta.error else paleta.texto,
        )
        Text(
            "L",
            style = EstilosAgua.UnidadCifra,
            color = paleta.texto2,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
        )
    }
}
