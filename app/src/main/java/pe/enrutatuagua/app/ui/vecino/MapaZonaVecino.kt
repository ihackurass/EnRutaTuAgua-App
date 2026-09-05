package pe.enrutatuagua.app.ui.vecino

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.color
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.lineDasharray
import org.maplibre.android.style.sources.GeoJsonSource
import pe.enrutatuagua.app.ui.mapa.EstiloMapa
import pe.enrutatuagua.data.ConectividadRepositoryAndroid
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.domain.modelo.EstadoValidacionPunto
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.modelo.CisternaEnZona
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mapa del vecino (plan 05 T2 S4): mismo lienzo Protomaps offline que
 * `MapaRuta` (mismo `EstiloMapa`, mismo ciclo de vida de `MapView`), pero con
 * una capa de marcadores distinta — los puntos de la zona (solo lectura) más
 * la última posición conocida de cada cisterna. Cada actualización GPS se
 * interpola hasta la siguiente coordenada y el sprite 3D se selecciona entre
 * 16 orientaciones según el rumbo real; no se inventan posiciones futuras.
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
    MapaZonaVecinoPlural(
        puntos,
        listOfNotNull(posicion?.let { CisternaEnZona(0, 0, it.capacidadLitros, it) }),
        modifier, oscuro, rumboCisterna, margenSuperiorCamara,
    )
}

@Composable
fun MapaZonaVecinoPlural(
    puntos: List<PuntoAbastecimiento>,
    cisternas: List<CisternaEnZona>,
    modifier: Modifier = Modifier,
    oscuro: Boolean = isSystemInDarkTheme(),
    rumboCisterna: Float? = null,
    margenSuperiorCamara: Dp = 0.dp,
) {
    val contexto = LocalContext.current
    val paleta = LocalPaletaAgua.current
    val density = LocalDensity.current
    val posiciones = cisternas.mapNotNull { it.posicion }
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
    val claveEncuadre = remember { arrayOf<Pair<List<Long>, List<Long>>?>(null) }
    val mapaActual = remember { arrayOf<MapLibreMap?>(null) }
    val posicionesDibujadas = remember { mutableMapOf<Long, PosicionCisterna>() }
    val objetivos = remember { mutableMapOf<Long, PosicionCisterna>() }
    val rumbos = remember { mutableMapOf<Long, Float>() }
    val animadores = remember { mutableMapOf<Long, ValueAnimator>() }
    val puntosActuales = remember { arrayOf(puntos) }
    val cisternasActuales = remember { arrayOf(cisternas) }
    puntosActuales[0] = puntos
    cisternasActuales[0] = cisternas

    fun cisternasDibujadas(): List<CisternaEnZona> = cisternasActuales[0].map { cisterna ->
        cisterna.copy(posicion = posicionesDibujadas[cisterna.rutaId] ?: cisterna.posicion)
    }

    fun actualizarFuente() {
        val fuente = mapaActual[0]?.style?.getSource("zona") as? GeoJsonSource ?: return
        fuente.setGeoJson(geoJsonDeZonaPlural(puntosActuales[0], cisternasDibujadas(), rumboCisterna, rumbos))
    }

    fun animarHaciaObjetivos() {
        val idsVigentes = cisternasActuales[0].map { it.rutaId }.toSet()
        (posicionesDibujadas.keys - idsVigentes).forEach {
            animadores.remove(it)?.cancel(); posicionesDibujadas.remove(it); objetivos.remove(it); rumbos.remove(it)
        }
        cisternasActuales[0].forEach { cisterna ->
            val nueva = cisterna.posicion ?: return@forEach
            val anteriorObjetivo = objetivos[cisterna.rutaId]
            if (anteriorObjetivo?.lat == nueva.lat && anteriorObjetivo.lng == nueva.lng) return@forEach
            objetivos[cisterna.rutaId] = nueva
            val inicio = posicionesDibujadas[cisterna.rutaId]
            if (inicio == null) {
                posicionesDibujadas[cisterna.rutaId] = nueva
                actualizarFuente()
                return@forEach
            }
            rumbos[cisterna.rutaId] = rumboEntre(inicio, nueva)
            animadores.remove(cisterna.rutaId)?.cancel()
            animadores[cisterna.rutaId] = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = (nueva.timestampMs - inicio.timestampMs).coerceIn(900L, 5_000L)
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener { animacion ->
                    val avance = animacion.animatedValue as Float
                    posicionesDibujadas[cisterna.rutaId] = nueva.copy(
                        lat = inicio.lat + (nueva.lat - inicio.lat) * avance,
                        lng = inicio.lng + (nueva.lng - inicio.lng) * avance,
                    )
                    actualizarFuente()
                }
                start()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            animadores.values.forEach(ValueAnimator::cancel)
            mapView.onPause(); mapView.onStop(); mapView.onDestroy()
        }
    }

    AndroidView(modifier = modifier, factory = { mapView }, update = { vista ->
        vista.getMapAsync { mapa ->
            mapaActual[0] = mapa
            with(density) { mapa.setPadding(0, margenSuperiorCamara.toPx().toInt(), 0, 0) }
            val modo = oscuro to online
            if (modoAplicado[0] != modo) {
                modoAplicado[0] = modo
                mapa.setLatLngBoundsForCameraTarget(null)
                mapa.setMinZoomPreference(EstiloMapa.ZOOM_MIN_ONLINE)
                mapa.setStyle(Style.Builder().fromUri(EstiloMapa.URL_ONLINE)) { estilo ->
                    animadores.values.forEach(ValueAnimator::cancel)
                    posicionesDibujadas.clear(); objetivos.clear(); rumbos.clear()
                    cisternas.forEach { it.posicion?.let { posicion -> posicionesDibujadas[it.rutaId] = posicion; objetivos[it.rutaId] = posicion } }
                    estilo.addSource(GeoJsonSource("zona", geoJsonDeZonaPlural(puntos, cisternasDibujadas(), rumboCisterna, rumbos)))
                    recursosSpriteCisterna.forEach { (nombre, recurso) ->
                        estilo.addImage(nombre, BitmapFactory.decodeResource(contexto.resources, recurso))
                    }


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
                        iconImage(get("sprite")), iconSize(0.44f), iconAllowOverlap(true),
                        iconRotationAlignment("map"),
                    )
                    estilo.addLayer(cisterna)
                }
            } else {
                animarHaciaObjetivos()
                actualizarFuente()
                (mapa.style?.getLayer("cisterna-halo") as? CircleLayer)?.setProperties(circleRadius(pulso))
            }

            val clave = puntos.map { it.id } to cisternas.map { it.rutaId }
            if (claveEncuadre[0] != clave) {
                claveEncuadre[0] = clave
                val ubicaciones = puntos.map { LatLng(it.lat, it.lng) } + posiciones.map { LatLng(it.lat, it.lng) }
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

private fun rumboEntre(origen: PosicionCisterna, destino: PosicionCisterna): Float {
    val lat1 = Math.toRadians(origen.lat)
    val lat2 = Math.toRadians(destino.lat)
    val deltaLng = Math.toRadians(destino.lng - origen.lng)
    val y = sin(deltaLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng)
    return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
}

private val recursosSpriteCisterna = listOf(
    "cisterna-000" to pe.enrutatuagua.app.R.drawable.cisterna_000,
    "cisterna-022" to pe.enrutatuagua.app.R.drawable.cisterna_022,
    "cisterna-045" to pe.enrutatuagua.app.R.drawable.cisterna_045,
    "cisterna-067" to pe.enrutatuagua.app.R.drawable.cisterna_067,
    "cisterna-090" to pe.enrutatuagua.app.R.drawable.cisterna_090,
    "cisterna-112" to pe.enrutatuagua.app.R.drawable.cisterna_112,
    "cisterna-135" to pe.enrutatuagua.app.R.drawable.cisterna_135,
    "cisterna-157" to pe.enrutatuagua.app.R.drawable.cisterna_157,
    "cisterna-180" to pe.enrutatuagua.app.R.drawable.cisterna_180,
    "cisterna-202" to pe.enrutatuagua.app.R.drawable.cisterna_202,
    "cisterna-225" to pe.enrutatuagua.app.R.drawable.cisterna_225,
    "cisterna-247" to pe.enrutatuagua.app.R.drawable.cisterna_247,
    "cisterna-270" to pe.enrutatuagua.app.R.drawable.cisterna_270,
    "cisterna-292" to pe.enrutatuagua.app.R.drawable.cisterna_292,
    "cisterna-315" to pe.enrutatuagua.app.R.drawable.cisterna_315,
    "cisterna-337" to pe.enrutatuagua.app.R.drawable.cisterna_337,
)
