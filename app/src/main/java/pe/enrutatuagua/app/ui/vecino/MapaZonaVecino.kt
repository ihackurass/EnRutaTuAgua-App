package pe.enrutatuagua.app.ui.vecino

import android.graphics.BitmapFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.color
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.sources.GeoJsonSource
import pe.enrutatuagua.app.ui.mapa.EstiloMapa
import pe.enrutatuagua.data.ConectividadRepositoryAndroid
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.domain.modelo.EstadoValidacionPunto
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento

/**
 * Mapa del vecino (plan 05 T2 S4): mismo lienzo Protomaps offline que
 * `MapaRuta` (mismo `EstiloMapa`, mismo ciclo de vida de `MapView`), pero con
 * una capa de marcadores distinta — los puntos de la zona (solo lectura) más
 * un pin fijo de la última posición conocida de la cisterna. Sin línea de
 * ruta y sin animación: es una posición fija, no una simulación en vivo (§0).
 *
 * Nota de implementación: el ícono de camión (`TablerIcons.Truck`) que pide
 * el plan es un `ImageVector` de Compose; MapLibre (SDK nativo, no-Compose)
 * necesita un `Bitmap` rasterizado para íconos de marcador y este repo no
 * tiene todavía ese utilitario. Para no introducir una rasterización sin
 * verificar, el pin de la cisterna se distingue por color/tamaño con
 * `CircleLayer` — el mismo mecanismo, sin bitmaps, que ya usa `MapaRuta` para
 * todos sus marcadores.
 */
@Composable
fun MapaZonaVecino(
    puntos: List<PuntoAbastecimiento>,
    posicion: PosicionCisterna?,
    modifier: Modifier = Modifier,
    oscuro: Boolean = isSystemInDarkTheme(),
    rumboCisterna: Float? = null,
    margenSuperiorCamara: Dp = 0.dp,
) {
    val contexto = LocalContext.current
    val paleta = LocalPaletaAgua.current
    val density = LocalDensity.current
    var posicionMostrada by remember { mutableStateOf(posicion) }
    LaunchedEffect(posicion?.lat, posicion?.lng) {
        val destino = posicion
        val origen = posicionMostrada
        if (destino == null || origen == null) {
            posicionMostrada = destino
        } else {
            Animatable(0f).animateTo(1f, tween(800, easing = FastOutSlowInEasing)) {
                posicionMostrada = destino.copy(
                    lat = origen.lat + (destino.lat - origen.lat) * value,
                    lng = origen.lng + (destino.lng - origen.lng) * value,
                )
            }
        }
    }
    val pulso by rememberInfiniteTransition(label = "pulso-cisterna").animateFloat(
        initialValue = 25f,
        targetValue = 33f,
        animationSpec = infiniteRepeatable(tween(1_200), RepeatMode.Reverse),
        label = "radio-cisterna",
    )

    val cMarca = paleta.marca.toArgb()
    val cExito = paleta.exito.toArgb()
    val cHoja = paleta.hoja.toArgb()
    val cTrazo = paleta.mapaTrazo.toArgb()

    val mapView = remember {
        MapLibre.getInstance(contexto)
        MapView(contexto).apply { onCreate(null); onStart(); onResume() }
    }
    val conectividadRepo = remember { ConectividadRepositoryAndroid(contexto) }
    val online by conectividadRepo.observarConectividad().collectAsState(initial = false)
    val modoAplicado = remember { arrayOf<Pair<Boolean, Boolean>?>(null) }
    val claveEncuadre = remember { arrayOf<Pair<List<Long>, Boolean>?>(null) }

    DisposableEffect(Unit) {
        onDispose { mapView.onPause(); mapView.onStop(); mapView.onDestroy() }
    }

    AndroidView(modifier = modifier, factory = { mapView }, update = { vista ->
        vista.getMapAsync { mapa ->
            with(density) { mapa.setPadding(0, margenSuperiorCamara.toPx().toInt(), 0, 0) }
            val modo = oscuro to online
            if (modoAplicado[0] != modo) {
                modoAplicado[0] = modo
                mapa.setLatLngBoundsForCameraTarget(null)
                mapa.setMinZoomPreference(EstiloMapa.ZOOM_MIN_ONLINE)
                mapa.setStyle(Style.Builder().fromUri(EstiloMapa.URL_ONLINE)) { estilo ->
                    estilo.addSource(GeoJsonSource("zona", geoJsonDeZona(puntos, posicionMostrada, rumboCisterna)))
                    estilo.addImage(
                        "cisterna-icono",
                        BitmapFactory.decodeResource(contexto.resources, pe.enrutatuagua.app.R.drawable.ic_camion_cisterna),
                    )

                    val puntosCapa = CircleLayer("puntos-zona", "zona")
                    puntosCapa.setFilter(eq(get("tipo"), literal("punto")))
                    puntosCapa.setProperties(
                        circleRadius(14f),
                        circleColor(
                            switchCase(
                                eq(get("estado"), literal(EstadoValidacionPunto.VALIDADO.name)),
                                color(cExito),
                                color(cHoja),
                            ),
                        ),
                        circleStrokeWidth(3f),
                        circleStrokeColor(cTrazo),
                    )
                    estilo.addLayer(puntosCapa)

                    val cisternaHalo = CircleLayer("cisterna-halo", "zona")
                    cisternaHalo.setFilter(eq(get("tipo"), literal("cisterna")))
                    cisternaHalo.setProperties(
                        circleRadius(pulso), circleColor(cMarca), circleOpacity(0.18f),
                    )
                    estilo.addLayer(cisternaHalo)

                    val cisternaSombra = CircleLayer("cisterna-sombra", "zona")
                    cisternaSombra.setFilter(eq(get("tipo"), literal("cisterna")))
                    cisternaSombra.setProperties(
                        circleRadius(23f), circleColor(android.graphics.Color.BLACK), circleOpacity(0.28f),
                    )
                    estilo.addLayer(cisternaSombra)

                    val cisternaFondo = CircleLayer("cisterna-fondo", "zona")
                    cisternaFondo.setFilter(eq(get("tipo"), literal("cisterna")))
                    cisternaFondo.setProperties(
                        circleRadius(20f), circleColor(android.graphics.Color.WHITE),
                        circleStrokeWidth(2f), circleStrokeColor(cMarca),
                    )
                    estilo.addLayer(cisternaFondo)

                    val cisterna = SymbolLayer("cisterna", "zona")
                    cisterna.setFilter(eq(get("tipo"), literal("cisterna")))
                    cisterna.setProperties(
                        iconImage("cisterna-icono"), iconSize(0.85f), iconAllowOverlap(true),
                        iconRotate(get("rumbo")), iconRotationAlignment("map"),
                    )
                    estilo.addLayer(cisterna)
                }
            } else {
                (mapa.style?.getSource("zona") as? GeoJsonSource)?.setGeoJson(
                    geoJsonDeZona(puntos, posicionMostrada, rumboCisterna),
                )
                (mapa.style?.getLayer("cisterna-halo") as? CircleLayer)?.setProperties(circleRadius(pulso))
            }

            val clave = puntos.map { it.id } to (posicion != null)
            if (claveEncuadre[0] != clave) {
                claveEncuadre[0] = clave
                val ubicaciones = puntos.map { LatLng(it.lat, it.lng) } +
                    listOfNotNull(posicion?.let { LatLng(it.lat, it.lng) })
                if (ubicaciones.size == 1) {
                    mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicaciones.single(), 15.0))
                } else if (ubicaciones.isNotEmpty()) {
                    val bounds = LatLngBounds.Builder()
                    ubicaciones.forEach { bounds.include(it) }
                    with(density) {
                        mapa.moveCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                bounds.build(),
                                40.dp.toPx().toInt(), 150.dp.toPx().toInt(),
                                40.dp.toPx().toInt(), 286.dp.toPx().toInt(),
                            ),
                        )
                    }
                }
            }
        }
    })
}
