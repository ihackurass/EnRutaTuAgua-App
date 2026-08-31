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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.app.R
import pe.enrutatuagua.app.ui.vecino.MapaZonaVecino
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.BotonSecundario
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.app.ubicacion.rememberPermisoUbicacion
import java.text.DateFormat
import java.util.Date

@Composable
fun PruebaTrackingScreen(
    onVolver: () -> Unit,
    vm: PruebaTrackingViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsState()
    val paleta = LocalPaletaAgua.current
    val permiso = rememberPermisoUbicacion()
    var esperandoPermiso by remember { mutableStateOf(false) }
    LaunchedEffect(permiso.concedido, esperandoPermiso) {
        if (esperandoPermiso && permiso.concedido) {
            esperandoPermiso = false
            vm.alternar()
        }
    }
    Box(Modifier.fillMaxSize()) {
        MapaZonaVecino(
            emptyList(), estado.posicionRecibida, Modifier.fillMaxSize(),
            rumboCisterna = estado.rumbo,
            margenSuperiorCamara = 390.dp,
        )
        Column(
            Modifier.align(Alignment.TopCenter).padding(20.dp).fillMaxWidth()
                .background(paleta.hoja).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Simulación cerca de tu ubicación", style = EstilosAgua.TituloParada, color = paleta.texto)
            Text(
                "El movimiento es artificial; el modo Aguatero usa el GPS real.",
                style = EstilosAgua.Cuerpo, color = paleta.texto2,
            )
            Text(estado.mensaje, style = EstilosAgua.Cuerpo, color = paleta.texto2)
            estado.ancla?.let {
                Text("Inicio: %.5f, %.5f".format(it.lat, it.lng), style = EstilosAgua.Cuerpo, color = paleta.texto)
            }
            Text("Publicaciones confirmadas: ${estado.enviados}", style = EstilosAgua.Cuerpo, color = paleta.texto)
            Text(
                if (estado.posicionRecibida == null) "Backend: sin posición" else "Backend: posición recibida ✓",
                style = EstilosAgua.Cuerpo,
                color = paleta.texto,
            )
            estado.ultimaRecepcionMs?.let {
                Text(
                    "Última recepción: ${DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(it))}",
                    style = EstilosAgua.Cuerpo, color = paleta.texto2,
                )
            }
            BotonPrimario(
                if (estado.activa) "Detener simulación" else "Iniciar cerca de mí",
                iconoPainter = if (estado.activa) null else painterResource(R.drawable.ic_ubicacion_centrar),
                onClick = {
                    if (estado.activa) vm.detener()
                    else if (permiso.concedido) vm.alternar()
                    else {
                        esperandoPermiso = true
                        vm.permisoRequerido()
                        permiso.solicitar()
                    }
                },
            )
            BotonSecundario("Volver", onClick = { vm.detener(); onVolver() })
        }
    }
}
