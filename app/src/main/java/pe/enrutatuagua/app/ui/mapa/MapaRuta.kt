package pe.enrutatuagua.app.ui.mapa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import pe.enrutatuagua.domain.modelo.Ruta

private const val ESTILO_DEMO = "https://demotiles.maplibre.org/style.json"

@Composable
fun MapaRuta(ruta: Ruta, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(contexto)
        MapView(contexto).apply { onCreate(null); onStart(); onResume() }
    }
    DisposableEffect(Unit) {
        onDispose { mapView.onPause(); mapView.onStop(); mapView.onDestroy() }
    }
    AndroidView(modifier = modifier, factory = { mapView }, update = { vista ->
        vista.getMapAsync { mapa ->
            mapa.setStyle(ESTILO_DEMO) { estilo ->
                val fuente = estilo.getSource("ruta") as? GeoJsonSource
                if (fuente == null) {
                    estilo.addSource(GeoJsonSource("ruta", geoJsonDeRuta(ruta)))
                    estilo.addLayer(LineLayer("ruta-linea", "ruta").withProperties(lineColor("#1565C0"), lineWidth(4f)))
                    estilo.addLayer(CircleLayer("ruta-puntos", "ruta").withProperties(circleRadius(7f), circleColor("#E65100")))
                } else fuente.setGeoJson(geoJsonDeRuta(ruta))
            }
            val bounds = LatLngBounds.Builder()
            ruta.paradas.forEach { bounds.include(LatLng(it.punto.lat, it.punto.lng)) }
            mapa.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80))
        }
    })
}
