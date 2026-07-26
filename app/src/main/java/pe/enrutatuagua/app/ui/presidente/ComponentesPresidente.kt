package pe.enrutatuagua.app.ui.presidente

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

/**
 * Mismo patrón que `TarjetaError` de `ui/parada/ParadaScreen.kt` (plan 07 T7).
 * Se reimplementa aquí en lugar de moverla a un archivo compartido porque
 * esta ejecución (plan 06, rol Presidente) tiene prohibido tocar cualquier
 * archivo fuera de `ui/presidente/` — otro agente trabaja en paralelo sobre
 * el rol Vecino en el mismo repositorio (worktree separado).
 */
@Composable
internal fun TarjetaError(mensaje: String) {
    val paleta = LocalPaletaAgua.current
    val forma = RoundedCornerShape(RadiosAgua.Aviso)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(paleta.alerta.copy(alpha = 0.12f))
            .border(1.dp, paleta.alerta.copy(alpha = 0.4f), forma)
            .padding(12.dp),
    ) {
        Text(mensaje, style = EstilosAgua.Error, color = paleta.alerta)
    }
}
