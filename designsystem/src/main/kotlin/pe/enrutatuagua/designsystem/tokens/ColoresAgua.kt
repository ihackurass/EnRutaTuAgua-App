package pe.enrutatuagua.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta "sol de mediodía". Fuente de verdad: prototipo enrutatuagua-ui.html
 * (ANDROID-PLAN-07 §2.1). Contraste alto: la app se usa bajo sol directo.
 */
@Immutable
data class PaletaAgua(
    val marca: Color, val accion: Color, val alerta: Color, val exito: Color,
    val texto: Color, val texto2: Color, val linea: Color,
    val hoja: Color, val fondo: Color, val tenue: Color,
    val mapaBase: Color, val mapaManzana: Color, val mapaTrazo: Color,
    val mapaVia: Color, val mapaAgua: Color,
    val esClara: Boolean,
)

val PaletaClara = PaletaAgua(
    marca = Color(0xFF0D3B66), accion = Color(0xFF12A5B0),
    alerta = Color(0xFFFF6B1A), exito = Color(0xFF1E9E5A),
    texto = Color(0xFF0A1620), texto2 = Color(0xFF526B7C), linea = Color(0xFFDBE4EA),
    hoja = Color(0xFFFFFFFF), fondo = Color(0xFFD9E3E9), tenue = Color(0xFFEFF4F7),
    mapaBase = Color(0xFFE7EEF1), mapaManzana = Color(0xFFF7F9FA),
    mapaTrazo = Color(0xFFD3DDE3), mapaVia = Color(0xFFFFFFFF), mapaAgua = Color(0xFFCDE0EA),
    esClara = true,
)

val PaletaOscura = PaletaAgua(
    marca = Color(0xFF9AD1EA), accion = Color(0xFF2ABFC9),
    alerta = Color(0xFFFF8A44), exito = Color(0xFF35C179),
    texto = Color(0xFFE9F2F7), texto2 = Color(0xFF92AEBE), linea = Color(0xFF1E3A4B),
    hoja = Color(0xFF102532), fondo = Color(0xFF061119), tenue = Color(0xFF16303F),
    mapaBase = Color(0xFF0B1B24), mapaManzana = Color(0xFF10242F),
    mapaTrazo = Color(0xFF1A3543), mapaVia = Color(0xFF1E3F51), mapaAgua = Color(0xFF0E2233),
    esClara = false,
)

val LocalPaletaAgua = staticCompositionLocalOf { PaletaClara }
