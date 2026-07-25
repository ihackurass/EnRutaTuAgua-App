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

// Fuentes variables (OFL): Archivo (ancho 125 = Expanded) para display/cifras,
// Public Sans para el cuerpo. ANDROID-PLAN-07 §2.2.
val ArchivoExpandida = FontFamily(
    Font(R.font.archivo_variable, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.width(125f))),
    Font(R.font.archivo_variable, weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800), FontVariation.width(125f))),
)

val PublicSans = FontFamily(
    Font(R.font.public_sans_variable, weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.public_sans_variable, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.public_sans_variable, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.public_sans_variable, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private const val TNUM = "tnum"   // numerales tabulares: los litros siempre alinean

object EstilosAgua {
    val CifraHero = TextStyle(fontFamily = ArchivoExpandida, fontWeight = FontWeight.ExtraBold,
        fontSize = 58.sp, lineHeight = 58.sp, letterSpacing = (-0.05).em, fontFeatureSettings = TNUM)
    val TituloParada = TextStyle(fontFamily = ArchivoExpandida, fontWeight = FontWeight.ExtraBold,
        fontSize = 27.sp, lineHeight = 28.sp, letterSpacing = (-0.035).em)
    val ColumnaCifra = TextStyle(fontFamily = ArchivoExpandida, fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp, lineHeight = 26.sp, letterSpacing = (-0.045).em, fontFeatureSettings = TNUM)
    val DatoResumen = TextStyle(fontFamily = ArchivoExpandida, fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp, lineHeight = 24.sp, letterSpacing = (-0.04).em, fontFeatureSettings = TNUM)
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
