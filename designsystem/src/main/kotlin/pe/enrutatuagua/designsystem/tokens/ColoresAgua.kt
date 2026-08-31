package pe.enrutatuagua.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta "tinta y trazo" (ANDROID-PLAN-08, dirección Uber Base). Contraste alto:
 * la app se usa bajo sol directo. `tinta` es el CTA (en oscuro se invierte a
 * casi-blanco); `accion` es el ÚNICO acento saturado; `marca` queda solo como
 * identidad (splash, gota del selector) — fuera de los botones.
 */
@Immutable
data class PaletaAgua(
    val marca: Color, val accion: Color, val alerta: Color, val exito: Color,
    val texto: Color, val texto2: Color, val linea: Color,
    val hoja: Color, val fondo: Color, val tenue: Color,
    val tinta: Color, val sobreTinta: Color, val superficie2: Color, val error: Color,
    val mapaBase: Color, val mapaManzana: Color, val mapaTrazo: Color,
    val mapaVia: Color, val mapaAgua: Color,
    val esClara: Boolean,
)

val PaletaClara = PaletaAgua(
    marca = Color(0xFF005B6B), accion = Color(0xFF007C91),
    alerta = Color(0xFFC65D00), exito = Color(0xFF16834A),
    texto = Color(0xFF101820), texto2 = Color(0xFF5E6266), linea = Color(0xFFD9DDE0),
    hoja = Color(0xFFFFFFFF), fondo = Color(0xFFF5F5F5), tenue = Color(0xFFEEEEEE),
    tinta = Color(0xFF101820), sobreTinta = Color(0xFFFFFFFF),
    superficie2 = Color(0xFFE7E9EA), error = Color(0xFFC62828),
    mapaBase = Color(0xFFE8EAED), mapaManzana = Color(0xFFDDE1E5),
    mapaTrazo = Color(0xFFCBD2D8), mapaVia = Color(0xFFFFFFFF), mapaAgua = Color(0xFFC3DCE8),
    esClara = true,
)

val PaletaOscura = PaletaAgua(
    marca = Color(0xFF58CAD7), accion = Color(0xFF31B7C7),
    alerta = Color(0xFFFFA24C), exito = Color(0xFF43B978),
    texto = Color(0xFFF5F5F5), texto2 = Color(0xFFB3B6B8), linea = Color(0xFF34383B),
    hoja = Color(0xFF181A1B), fondo = Color(0xFF101112), tenue = Color(0xFF25282A),
    tinta = Color(0xFFF5F5F5), sobreTinta = Color(0xFF101820),
    superficie2 = Color(0xFF2C3032), error = Color(0xFFFF6B64),
    mapaBase = Color(0xFF12181D), mapaManzana = Color(0xFF1A2228),
    mapaTrazo = Color(0xFF242E36), mapaVia = Color(0xFF2B3740), mapaAgua = Color(0xFF142A36),
    esClara = false,
)

val LocalPaletaAgua = staticCompositionLocalOf { PaletaClara }
