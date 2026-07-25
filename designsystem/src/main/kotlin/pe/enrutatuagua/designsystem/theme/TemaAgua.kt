package pe.enrutatuagua.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import pe.enrutatuagua.designsystem.tokens.FormasAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.PaletaClara
import pe.enrutatuagua.designsystem.tokens.PaletaOscura
import pe.enrutatuagua.designsystem.tokens.TipografiaAgua

@Composable
fun TemaAgua(oscuro: Boolean = isSystemInDarkTheme(), contenido: @Composable () -> Unit) {
    val paleta = if (oscuro) PaletaOscura else PaletaClara
    val esquema = if (oscuro) darkColorScheme(
        primary = paleta.marca, onPrimary = paleta.fondo,
        secondary = paleta.accion, onSecondary = paleta.fondo,
        error = paleta.alerta, onError = paleta.fondo,
        background = paleta.fondo, onBackground = paleta.texto,
        surface = paleta.hoja, onSurface = paleta.texto,
        surfaceVariant = paleta.tenue, onSurfaceVariant = paleta.texto2,
        outline = paleta.linea,
    ) else lightColorScheme(
        primary = paleta.marca, onPrimary = paleta.hoja,
        secondary = paleta.accion, onSecondary = paleta.hoja,
        error = paleta.alerta, onError = paleta.hoja,
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
