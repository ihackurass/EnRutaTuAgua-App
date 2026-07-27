package pe.enrutatuagua.app.ui.mapa

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.all
import org.maplibre.android.style.expressions.Expression.color
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.geometryType
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import pe.enrutatuagua.app.R
import pe.enrutatuagua.app.ubicacion.MotorPosicionConRumbo
import pe.enrutatuagua.app.ubicacion.MotorPosicionGps
import pe.enrutatuagua.app.ubicacion.rememberPermisoUbicacion
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.MovimientoAgua
import pe.enrutatuagua.domain.modelo.Ruta

/**
 * Mapa como lienzo (plan 07 T5): estilo Protomaps offline con los tokens del
 * design system, ruta con halo, pines por estado y latido en la próxima parada.
 * Cámara con padding asimétrico para que la hoja (262) y la columna no tapen la ruta.
 *
 * Camión en vivo (plan 09): LocationComponent de MapLibre con el ícono cenital
 * del camión, rotado por rumbo (RenderMode.GPS) y alimentado por
 * [MotorPosicionConRumbo]; FAB para alternar seguimiento tipo Uber
 * (TRACKING_GPS, tilt 55°) ↔ encuadre completo. El wiring MapLibre nativo no
 * corre en Robolectric (RutaScreenTest stubea este slot); su contrato se
 * verifica e2e en emulador.
 */
@Composable
fun MapaRuta(ruta: Ruta, modifier: Modifier = Modifier, oscuro: Boolean = isSystemInDarkTheme()) {
    val contexto = LocalContext.current
    val paleta = LocalPaletaAgua.current
    val density = LocalDensity.current

    val cAccion = paleta.accion.toArgb()
    val cExito = paleta.exito.toArgb()
    val cAlerta = paleta.alerta.toArgb()
    val cHoja = paleta.hoja.toArgb()
    val cTrazo = paleta.mapaTrazo.toArgb()
    val cTexto2 = paleta.texto2.toArgb()

    val permiso = rememberPermisoUbicacion()
    LaunchedEffect(Unit) { if (!permiso.concedido) permiso.solicitar() }
    val motor = remember {
        MotorPosicionConRumbo(MotorPosicionGps(contexto))
    }
    var siguiendo by rememberSaveable { mutableStateOf(false) }
    val escuchaSeguimiento = remember {
        object : OnCameraTrackingChangedListener {
            // Un gesto sobre el mapa rompe el seguimiento: el FAB vuelve a "Seguir camión".
            override fun onCameraTrackingDismissed() {
                siguiendo = false
            }

            override fun onCameraTrackingChanged(modo: Int) = Unit
        }
    }

    val mapView = remember {
        MapLibre.getInstance(contexto)
        MapView(contexto).apply { onCreate(null); onStart(); onResume() }
    }
    val latido = remember {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = MovimientoAgua.Latido.toLong()
            repeatCount = ValueAnimator.INFINITE
        }
    }
    val oscuroAplicado = remember { arrayOf<Boolean?>(null) }
    val encuadreAplicado = remember { arrayOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            latido.cancel()
            mapView.onPause(); mapView.onStop(); mapView.onDestroy()
        }
    }

    Box(modifier) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView }, update = { vista ->
            // Leídos aquí a propósito: al cambiar cualquiera de los dos,
            // Compose re-ejecuta update sin recargar el estilo.
            val concedido = permiso.concedido
            val sigue = siguiendo
            vista.getMapAsync { mapa ->
                // Brújula abajo a la izquierda, sobre la hoja: arriba a la
                // derecha chocaba con el chip de estado y la columna de agua.
                mapa.uiSettings.setCompassGravity(Gravity.BOTTOM or Gravity.START)
                with(density) {
                    mapa.uiSettings.setCompassMargins(
                        14.dp.toPx().toInt(), 0, 0, 286.dp.toPx().toInt(),
                    )
                }
                if (oscuroAplicado[0] != oscuro) {
                    oscuroAplicado[0] = oscuro
                    mapa.setStyle(Style.Builder().fromJson(EstiloMapa.json(contexto, oscuro))) { estilo ->
                        estilo.addSource(GeoJsonSource("ruta", geoJsonDeRuta(ruta)))
                        estilo.addLayer(
                            LineLayer("ruta-halo", "ruta").withProperties(
                                lineColor(cAccion), lineOpacity(0.18f), lineWidth(13f),
                                lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
                            ),
                        )
                        estilo.addLayer(
                            LineLayer("ruta-linea", "ruta").withProperties(
                                lineColor(cAccion), lineWidth(5f),
                                lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
                            ),
                        )
                        val latidoCapa = CircleLayer("latido", "ruta")
                        latidoCapa.setFilter(all(eq(geometryType(), literal("Point")), eq(get("proxima"), literal(true))))
                        latidoCapa.setProperties(
                            circleColor(cAccion), circleOpacity(0f),
                            circleStrokeColor(cAccion), circleStrokeWidth(2f), circleRadius(10f),
                        )
                        estilo.addLayer(latidoCapa)

                        val paradas = CircleLayer("paradas", "ruta")
                        paradas.setFilter(eq(geometryType(), literal("Point")))
                        paradas.setProperties(
                            circleRadius(switchCase(eq(get("proxima"), literal(true)), literal(17f), literal(14f))),
                            circleColor(
                                switchCase(
                                    eq(get("estado"), literal("ENTREGADO")), color(cExito),
                                    eq(get("estado"), literal("OMITIDO")), color(cAlerta),
                                    eq(get("proxima"), literal(true)), color(cAccion),
                                    color(cHoja),
                                ),
                            ),
                            circleStrokeWidth(3f),
                            circleStrokeColor(
                                switchCase(
                                    all(eq(get("estado"), literal("PENDIENTE")), eq(get("proxima"), literal(false))),
                                    color(cTrazo),
                                    color(cHoja),
                                ),
                            ),
                        )
                        estilo.addLayer(paradas)

                        val orden = SymbolLayer("paradas-orden", "ruta")
                        orden.setFilter(eq(geometryType(), literal("Point")))
                        orden.setProperties(
                            textField(get("ordenTxt")),
                            textFont(arrayOf("Noto Sans Medium")),
                            textSize(12f),
                            textAllowOverlap(true),
                            textIgnorePlacement(true),
                            textColor(
                                switchCase(
                                    all(eq(get("estado"), literal("PENDIENTE")), eq(get("proxima"), literal(false))),
                                    color(cTexto2),
                                    color(android.graphics.Color.WHITE),
                                ),
                            ),
                        )
                        estilo.addLayer(orden)

                        latido.removeAllUpdateListeners()
                        latido.addUpdateListener { animador ->
                            val t = animador.animatedValue as Float
                            (estilo.getLayer("latido") as? CircleLayer)?.setProperties(
                                circleRadius(10f + 17f * t),
                                circleStrokeOpacity(0.85f * (1f - t)),
                            )
                        }
                        if (!latido.isStarted) latido.start()

                        // Re-setStyle (claro/oscuro) destruye las capas del puck:
                        // se re-activa SIEMPRE contra el estilo recién cargado.
                        activarPuck(mapa, estilo, contexto, motor, cAccion, concedido, escuchaSeguimiento)
                    }
                } else {
                    (mapa.style?.getSource("ruta") as? GeoJsonSource)?.setGeoJson(geoJsonDeRuta(ruta))
                    val estilo = mapa.style
                    if (concedido && estilo != null && !mapa.locationComponent.isLocationComponentActivated) {
                        // El permiso se concedió con el estilo ya cargado.
                        activarPuck(mapa, estilo, contexto, motor, cAccion, concedido, escuchaSeguimiento)
                    }
                }

                val puck = mapa.locationComponent
                if (sigue && puck.isLocationComponentActivated) {
                    if (puck.cameraMode != CameraMode.TRACKING_GPS) {
                        puck.cameraMode = CameraMode.TRACKING_GPS
                        puck.zoomWhileTracking(17.0)
                        puck.tiltWhileTracking(55.0)
                        with(density) {
                            // El camión queda visible por encima de la hoja.
                            puck.paddingWhileTracking(doubleArrayOf(0.0, 0.0, 0.0, 286.dp.toPx().toDouble()))
                        }
                    }
                } else {
                    if (puck.isLocationComponentActivated && puck.cameraMode != CameraMode.NONE) {
                        puck.cameraMode = CameraMode.NONE
                        // Al soltar el seguimiento se vuelve al encuadre de la ruta.
                        encuadreAplicado[0] = null
                    }
                    val clave = claveDeEncuadre(ruta)
                    if (encuadreAplicado[0] != clave) {
                        encuadreAplicado[0] = clave
                        val bounds = LatLngBounds.Builder()
                        ruta.paradas.forEach { bounds.include(LatLng(it.punto.lat, it.punto.lng)) }
                        with(density) {
                            mapa.moveCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    bounds.build(),
                                    40.dp.toPx().toInt(), 120.dp.toPx().toInt(),
                                    96.dp.toPx().toInt(), 286.dp.toPx().toInt(),
                                ),
                            )
                        }
                    }
                }
            }
        })

        if (permiso.concedido) {
            BotonSeguirCamion(
                siguiendo = siguiendo,
                onClick = { siguiendo = !siguiendo },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 276.dp),
            )
        }
    }
}

@SuppressLint("MissingPermission") // guardado por el chequeo explícito de `concedido`
private fun activarPuck(
    mapa: MapLibreMap,
    estilo: Style,
    contexto: Context,
    motor: MotorPosicionConRumbo,
    colorPrecision: Int,
    concedido: Boolean,
    escuchaSeguimiento: OnCameraTrackingChangedListener,
) {
    if (!concedido) return
    val puck = mapa.locationComponent
    puck.activateLocationComponent(
        LocationComponentActivationOptions.builder(contexto, estilo)
            .locationEngine(motor)
            .useDefaultLocationEngine(false)
            .useSpecializedLocationLayer(true)
            .locationEngineRequest(
                LocationEngineRequest.Builder(1000L)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .setFastestInterval(500L)
                    .build(),
            )
            .locationComponentOptions(
                LocationComponentOptions.builder(contexto)
                    .gpsDrawable(R.drawable.ic_camion_cisterna)
                    .foregroundDrawable(R.drawable.ic_camion_cisterna)
                    .accuracyColor(colorPrecision)
                    .accuracyAlpha(0.12f)
                    .trackingGesturesManagement(true)
                    .build(),
            )
            .build(),
    )
    puck.isLocationComponentEnabled = true
    puck.renderMode = RenderMode.GPS
    // Estable por composición; remove+add evita duplicarla en cada re-setStyle.
    puck.removeOnCameraTrackingChangedListener(escuchaSeguimiento)
    puck.addOnCameraTrackingChangedListener(escuchaSeguimiento)
}
