package pe.enrutatuagua.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ANDROID-PLAN-07 §2.3 — radios exactos del prototipo.
object RadiosAgua {
    val Hoja = 22.dp
    val Deslizador = 14.dp
    val BotonPrimario = 14.dp
    val Tarjeta = 13.dp
    val Motivo = 13.dp
    val Aviso = 13.dp
    val Tubo = 13.dp
    val Secundario = 12.dp
    val Comentario = 12.dp
    val Preset = 11.dp
    val Tecla = 11.dp
    val Perilla = 10.dp
    val Badge = 9.dp
    val Chip = 6.dp
    val Asa = 2.dp
}

val FormasAgua = Shapes(
    small = RoundedCornerShape(RadiosAgua.Chip),
    medium = RoundedCornerShape(RadiosAgua.Tarjeta),
    large = RoundedCornerShape(topStart = RadiosAgua.Hoja, topEnd = RadiosAgua.Hoja),
)
