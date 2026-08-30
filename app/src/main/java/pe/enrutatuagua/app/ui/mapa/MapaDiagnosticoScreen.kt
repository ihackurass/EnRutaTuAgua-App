package pe.enrutatuagua.app.ui.mapa

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import pe.enrutatuagua.designsystem.tokens.EstilosAgua

/**
 * Mapa "pelado" -- sin HojaArrastrable, sin encuadre por bounds, sin
 * LocationComponent, sin nada de la lógica de RutaScreen/MapaRuta. Sólo un
 * MapView con el mismo estilo/PMTiles que usa el resto de la app, centrado
 * con una CameraPosition fija (sin ningún cálculo de padding).
 *
 * Existe para aislar si "el mapa no carga ciertas partes" es un problema
 * del dato (PMTiles) en sí o de la lógica de cámara de la app -- ya se
 * encontró y corrigió un bug de padding de cámara en MapaRuta.kt que
 * causaba exactamente ese síntoma (encuadraba una zona random de Lima
 * distinta en cada dispositivo); esta pantalla sirve para confirmar que acá,
 * sin esa lógica, el mapa carga completo -- si el mismo problema apareciera
 * incluso acá, la causa sería el dato, no la cámara.
 */
@Composable
fun MapaDiagnosticoScreen(oscuro: Boolean = isSystemInDarkTheme()) {
    val contexto = LocalContext.current
    var permisoConcedido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permisoConcedido = it
    }
    LaunchedEffect(Unit) { if (!permisoConcedido) lanzador.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    val mapView = remember {
        MapLibre.getInstance(contexto)
        MapView(contexto).apply { onCreate(null); onStart(); onResume() }
    }
    DisposableEffect(Unit) {
        onDispose { mapView.onPause(); mapView.onStop(); mapView.onDestroy() }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView }, update = { vista ->
            vista.getMapAsync { mapa ->
                mapa.setStyle(Style.Builder().fromUri(EstiloMapa.URL_ONLINE)) {
                    // Centro geométrico del bbox extraído en
                    // lima-sur.pmtiles (-76.99,-12.21 a -76.90,-12.12) --
                    // sin bounds-fit, sin padding, la posición de cámara
                    // más simple posible. Confirmado con Nominatim que estas
                    // coordenadas caen en "Nueva Esperanza, Villa María del
                    // Triunfo" -- coincide con lo que renderiza este mapa.
                    mapa.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(-12.165, -76.945))
                        .zoom(13.0)
                        .build()
                }
            }
        })
        Surface(
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp),
            color = androidx.compose.ui.graphics.Color.White,
        ) {
            Text(
                "DIAGNÓSTICO: mapa pelado, sin lógica de la app",
                style = EstilosAgua.ItemSub,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
