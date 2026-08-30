package pe.enrutatuagua.app.ui.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.app.ui.vecino.MapaZonaVecino
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.BotonSecundario
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

@Composable
fun PruebaTrackingScreen(
    onVolver: () -> Unit,
    vm: PruebaTrackingViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsState()
    val paleta = LocalPaletaAgua.current
    Box(Modifier.fillMaxSize()) {
        MapaZonaVecino(emptyList(), estado.posicionRecibida, Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.TopCenter).padding(20.dp).fillMaxWidth()
                .background(paleta.hoja).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Prueba tracking en vivo", style = EstilosAgua.TituloParada, color = paleta.texto)
            Text(estado.mensaje, style = EstilosAgua.Cuerpo, color = paleta.texto2)
            Text("Publicaciones confirmadas: ${estado.enviados}", style = EstilosAgua.Cuerpo, color = paleta.texto)
            Text(
                if (estado.posicionRecibida == null) "Backend: sin posición" else "Backend: posición recibida ✓",
                style = EstilosAgua.Cuerpo,
                color = paleta.texto,
            )
            BotonPrimario(if (estado.activa) "Detener simulación" else "Iniciar simulación", vm::alternar)
            BotonSecundario("Volver", onClick = { vm.detener(); onVolver() })
        }
    }
}
