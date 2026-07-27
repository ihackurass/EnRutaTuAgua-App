package pe.enrutatuagua.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import pe.enrutatuagua.designsystem.tokens.FormasAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.PaletaClara
import pe.enrutatuagua.designsystem.tokens.PaletaOscura
import pe.enrutatuagua.designsystem.tokens.TipografiaAgua

@Composable
fun TemaAgua(oscuro: Boolean = isSystemInDarkTheme(), contenido: @Composable () -> Unit) {
    val paleta = if (oscuro) PaletaOscura else PaletaClara
    val esquema = if (oscuro) darkColorScheme(
        primary = paleta.tinta, onPrimary = paleta.sobreTinta,
        secondary = paleta.accion, onSecondary = Color(0xFF0A1620),
        error = paleta.error, onError = paleta.sobreTinta,
        background = paleta.fondo, onBackground = paleta.texto,
        surface = paleta.hoja, onSurface = paleta.texto,
        surfaceVariant = paleta.tenue, onSurfaceVariant = paleta.texto2,
        outline = paleta.linea,
    ) else lightColorScheme(
        primary = paleta.tinta, onPrimary = paleta.sobreTinta,
        secondary = paleta.accion, onSecondary = Color.White,
        error = paleta.error, onError = paleta.sobreTinta,
        background = paleta.fondo, onBackground = paleta.texto,
        surface = paleta.hoja, onSurface = paleta.texto,
        surfaceVariant = paleta.tenue, onSurfaceVariant = paleta.texto2,
        outline = paleta.linea,
    )
    CompositionLocalProvider(LocalPaletaAgua provides paleta) {
        MaterialTheme(
            colorScheme = esquema,
            typography = TipografiaAgua,
            shapes = FormasAgua,
            content = contenido,
        )
    }
}
