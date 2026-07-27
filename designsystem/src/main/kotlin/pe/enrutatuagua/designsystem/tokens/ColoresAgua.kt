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
    marca = Color(0xFF0D3B66), accion = Color(0xFF0A7F90),
    alerta = Color(0xFFB85500), exito = Color(0xFF048848),
    texto = Color(0xFF0A1620), texto2 = Color(0xFF57666F), linea = Color(0xFFE3E7EA),
    hoja = Color(0xFFFFFFFF), fondo = Color(0xFFEDF0F2), tenue = Color(0xFFF6F8FA),
    tinta = Color(0xFF0E1B2A), sobreTinta = Color(0xFFFFFFFF),
    superficie2 = Color(0xFFF0F2F4), error = Color(0xFFE11900),
    mapaBase = Color(0xFFE8EAED), mapaManzana = Color(0xFFDDE1E5),
    mapaTrazo = Color(0xFFCBD2D8), mapaVia = Color(0xFFFFFFFF), mapaAgua = Color(0xFFC3DCE8),
    esClara = true,
)

val PaletaOscura = PaletaAgua(
    marca = Color(0xFF9AD1EA), accion = Color(0xFF2CC7D3),
    alerta = Color(0xFFFFB03A), exito = Color(0xFF06C167),
    texto = Color(0xFFE9F2F7), texto2 = Color(0xFF94A6B1), linea = Color(0xFF24313B),
    hoja = Color(0xFF101A21), fondo = Color(0xFF0B1318), tenue = Color(0xFF17222B),
    tinta = Color(0xFFE9F2F7), sobreTinta = Color(0xFF0A1620),
    superficie2 = Color(0xFF1D2933), error = Color(0xFFFF5C48),
    mapaBase = Color(0xFF12181D), mapaManzana = Color(0xFF1A2228),
    mapaTrazo = Color(0xFF242E36), mapaVia = Color(0xFF2B3740), mapaAgua = Color(0xFF142A36),
    esClara = false,
)

val LocalPaletaAgua = staticCompositionLocalOf { PaletaClara }
