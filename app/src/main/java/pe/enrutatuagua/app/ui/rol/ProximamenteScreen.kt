package pe.enrutatuagua.app.ui.rol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Server
import pe.enrutatuagua.designsystem.component.BotonSecundario
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

/**
 * Pantalla informativa del rol ADMIN (RD-01): en el MVP no tiene pantallas de
 * gestión — los datos maestros se cargan por el equipo técnico vía SQL seed,
 * y el panel de administración queda como app web futura. Único rol que aún
 * no tiene flujo propio (Vecino y Presidente ya lo tienen: planes 05 y 06).
 */
@Composable
fun ProximamenteScreen(onVolver: () -> Unit) {
    val paleta = LocalPaletaAgua.current
    Column(
        Modifier.fillMaxSize().background(paleta.fondo).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(TablerIcons.Server, contentDescription = null,
            tint = paleta.texto2, modifier = Modifier.size(64.dp))
        Text("Hola, Administrador", style = EstilosAgua.TituloParada,
            color = paleta.texto, textAlign = TextAlign.Center)
        Text(
            "En esta versión no tienes pantallas propias: los datos maestros " +
                "(usuarios, camiones, asociaciones) los carga el equipo técnico. " +
                "El panel de administración llega más adelante, como app web.",
            style = EstilosAgua.Cuerpo, color = paleta.texto2, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        BotonSecundario("Cambiar de rol", onClick = onVolver, modifier = Modifier.fillMaxWidth())
    }
}
