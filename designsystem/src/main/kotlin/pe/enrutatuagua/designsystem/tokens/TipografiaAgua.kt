@file:OptIn(ExperimentalTextApi::class)

package pe.enrutatuagua.designsystem.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pe.enrutatuagua.designsystem.R

// Una sola familia en toda la app: Public Sans (variable, OFL), ancho normal.
// El display/cifras se distingue por peso (ExtraBold) y tamaño, no por un eje
// de ancho expandido — el usuario lo rechazó al verlo en pantalla.
val PublicSans = FontFamily(
    Font(R.font.public_sans_variable, weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.public_sans_variable, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.public_sans_variable, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.public_sans_variable, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.public_sans_variable, weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

private const val TNUM = "tnum"   // numerales tabulares: los litros siempre alinean

object EstilosAgua {
    val CifraHero = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 58.sp, lineHeight = 58.sp, letterSpacing = (-0.03).em, fontFeatureSettings = TNUM)
    val TituloParada = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 27.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em)
    val ColumnaCifra = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.02).em, fontFeatureSettings = TNUM)
    val DatoResumen = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.02).em, fontFeatureSettings = TNUM)
    val TeclaDigito = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, fontFeatureSettings = TNUM)
    val UnidadCifra = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    val BotonPrimario = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, letterSpacing = (-0.01).em)
    val Zona = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, letterSpacing = (-0.01).em)
    val Cuerpo = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 21.sp)
    val ItemTitulo = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, letterSpacing = (-0.01).em)
    val Deslizador = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, fontFeatureSettings = TNUM)
    val Error = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp)
    val Preset = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, letterSpacing = (-0.02).em, fontFeatureSettings = TNUM)
    val ItemSub = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, fontFeatureSettings = TNUM)
    val Placa = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val PieColumna = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, fontFeatureSettings = TNUM)
    val Rotulo = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, letterSpacing = 0.13.em)
    val ChipEstado = TextStyle(fontFamily = PublicSans, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, letterSpacing = 0.08.em)
}

val TipografiaAgua = Typography(
    displayLarge = EstilosAgua.CifraHero,
    headlineMedium = EstilosAgua.TituloParada,
    titleMedium = EstilosAgua.Zona,
    bodyLarge = EstilosAgua.Cuerpo,
    bodySmall = EstilosAgua.ItemSub,
    labelLarge = EstilosAgua.BotonPrimario,
    labelSmall = EstilosAgua.Rotulo,
)
