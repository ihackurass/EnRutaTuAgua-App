package pe.enrutatuagua.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ANDROID-PLAN-08 §2.1 — radios moderados (dirección Uber Base).
object RadiosAgua {
    val Hoja = 18.dp
    val Deslizador = 12.dp
    val BotonPrimario = 12.dp
    val Tarjeta = 12.dp
    val Motivo = 12.dp
    val Aviso = 12.dp
    val Tubo = 13.dp // cápsula: mitad del ancho del tubo (26dp), no bajar
    val Secundario = 12.dp
    val Comentario = 10.dp
    val Preset = 10.dp
    val Tecla = 10.dp
    val Perilla = 8.dp
    val Badge = 8.dp
    val Chip = 6.dp
    val Asa = 2.dp
}

val FormasAgua = Shapes(
    small = RoundedCornerShape(RadiosAgua.Chip),
    medium = RoundedCornerShape(RadiosAgua.Tarjeta),
    large = RoundedCornerShape(topStart = RadiosAgua.Hoja, topEnd = RadiosAgua.Hoja),
)
