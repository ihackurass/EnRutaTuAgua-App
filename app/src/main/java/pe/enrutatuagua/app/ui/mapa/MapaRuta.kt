package pe.enrutatuagua.app.ui.mapa

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import org.maplibre.android.location.LocationComponentConstants
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
import org.maplibre.android.style.layers.Layer
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
import pe.enrutatuagua.data.ConectividadRepositoryAndroid
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
    // HojaArrastrable ahora descuenta la barra de navegación del sistema al
    // calcular su asomo (hallazgo real en dispositivo: el botón "No pude
    // entregar" quedaba tapado) -- el asomo sube esa misma franja, así que
    // todo lo que se apoyaba en el viejo margen fijo de 286/276dp (brújula,
    // FAB de seguimiento, padding de cámara) necesita subir lo mismo o
    // queda tapado por la hoja.
    val alturaBarraNav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val cAccion = paleta.accion.toArgb()
    val cTinta = paleta.tinta.toArgb()
    val cExito = paleta.exito.toArgb()
    val cAlerta = paleta.alerta.toArgb()
    val cHoja = paleta.hoja.toArgb()
    val cTrazo = paleta.mapaTrazo.toArgb()
    val cTexto2 = paleta.texto2.toArgb()

    val permiso = rememberPermisoUbicacion()
    LaunchedEffect(Unit) { if (!permiso.concedido) permiso.solicitar() }

    // Mapa híbrido online/offline (plan de precarga): el PMTiles bundleado
    // sigue siendo el piso garantizado sin internet (RNF-01); esto sólo
    // decide cuándo preferir el estilo online (más cobertura real) por
    // encima de ese piso. Instancia propia (este archivo no usa Hilt) --
    // mismo patrón que MotorPosicionGps más abajo.
    val conectividadRepo = remember { ConectividadRepositoryAndroid(contexto) }
    val online by conectividadRepo.observarConectividad().collectAsState(initial = false)
    val clave = claveDeEncuadre(ruta)
    // Una vez que hubo señal durante ESTA ruta, se sigue prefiriendo el
    // estilo online aunque la señal se corte después -- la precarga hecha
    // en background (PrecacheRutaObservador) sigue sirviendo tiles para
    // esa zona. Riesgo aceptado conscientemente (ver plan): si la señal se
    // corta ANTES de que la precarga termine, se ve el estilo online
    // parcialmente vacío en vez de caer al bundle offline garantizado.
    val huboConectividadEnEstaRuta = remember(clave) { mutableStateOf(false) }
    if (online) huboConectividadEnEstaRuta.value = true
    val usarOnline = online || huboConectividadEnEstaRuta.value

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
    val modoAplicado = remember { arrayOf<Pair<Boolean, Boolean>?>(null) }
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
                // Hallazgo real (captura en emulador AVD, panning manual):
                // fuera del bbox con el que se extrajo lima-sur.pmtiles
                // (-76.99,-12.21 a -76.90,-12.12) no hay NINGÚN dato -- se ve
                // mitad mapa/mitad gris con un corte recto, sin importar el
                // zoom (ahí no hay tiles a ningún nivel). setLatLngBoundsForCameraTarget
                // sólo frena dónde puede estar el CENTRO de la cámara; el borde
                // del viewport igual puede asomarse más allá del bbox con el
                // centro pegado al límite -- por eso el límite de paneo va con
                // margen hacia adentro, y se combina con un minZoom para que
                // el ancho visible en pantalla nunca supere ese margen.
                // Mapa híbrido: esta restricción sólo tiene sentido offline
                // (dataset chico y fijo); online (OpenFreeMap) hay cobertura
                // real en toda el área, así que se levanta.
                mapa.setLatLngBoundsForCameraTarget(null)
                mapa.setMinZoomPreference(EstiloMapa.ZOOM_MIN_ONLINE)
                mapa.uiSettings.setCompassGravity(Gravity.BOTTOM or Gravity.START)
                with(density) {
                    mapa.uiSettings.setCompassMargins(
                        14.dp.toPx().toInt(), 0, 0, (286.dp + alturaBarraNav).toPx().toInt(),
                    )
                }
                if (modoAplicado[0] != (oscuro to usarOnline)) {
                    modoAplicado[0] = oscuro to usarOnline
                    mapa.setStyle(Style.Builder().fromUri(EstiloMapa.URL_ONLINE)) { estilo ->
                        estilo.addSource(GeoJsonSource("ruta", geoJsonDeRuta(ruta)))
                        // Trazo en tinta (negro azulado; casi blanco en oscuro): el
                        // teal queda para el camión, pines y acentos, estilo Uber.
                        agregarDebajoDelCamion(
                            estilo,
                            LineLayer("ruta-halo", "ruta").withProperties(
                                lineColor(cTinta), lineOpacity(0.18f), lineWidth(13f),
                                lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
                            ),
                        )
                        agregarDebajoDelCamion(
                            estilo,
                            LineLayer("ruta-linea", "ruta").withProperties(
                                lineColor(cTinta), lineWidth(5f),
                                lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
                            ),
                        )
                        val latidoCapa = CircleLayer("latido", "ruta")
                        latidoCapa.setFilter(all(eq(geometryType(), literal("Point")), eq(get("proxima"), literal(true))))
                        latidoCapa.setProperties(
                            circleColor(cAccion), circleOpacity(0f),
                            circleStrokeColor(cAccion), circleStrokeWidth(2f), circleRadius(10f),
                        )
                        agregarDebajoDelCamion(estilo, latidoCapa)

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
                        agregarDebajoDelCamion(estilo, paradas)

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
                        agregarDebajoDelCamion(estilo, orden)

                        // Hallazgo real (crash en emulador): antes este
                        // listener capturaba `estilo` del closure. Con el
                        // mapa híbrido el estilo puede recargarse solo poco
                        // después de abrir la pantalla (offline -> online al
                        // resolverse la conectividad) -- si una animación en
                        // vuelo llamaba estilo.getLayer() sobre la instancia
                        // VIEJA justo cuando ya había una más nueva, MapLibre
                        // tira IllegalStateException. Se usa mapa.style
                        // (el estilo activo en ese momento), nunca uno capturado.
                        latido.removeAllUpdateListeners()
                        latido.addUpdateListener { animador ->
                            val t = animador.animatedValue as Float
                            (mapa.style?.getLayer("latido") as? CircleLayer)?.setProperties(
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
                        // Hallazgo real (reporte del usuario): con el camión
                        // detenido (sin rumbo real todavía), activar el
                        // seguimiento hacía saltar la cámara a norte de
                        // golpe, deshaciendo el encuadre izq→der. Se le pasa
                        // ESE bearing como rumbo de arranque -- en cuanto haya
                        // movimiento real, motor lo reemplaza solo.
                        motor.rumboInicial = bearingLecturaIzqADer(ruta)?.toFloat()
                        puck.cameraMode = CameraMode.TRACKING_GPS
                        puck.zoomWhileTracking(17.0)
                        puck.tiltWhileTracking(55.0)
                        with(density) {
                            // El camión queda visible por encima de la hoja.
                            // OJO: sumar alturaBarraNav acá rompió el encuadre
                            // del mapa (hallazgo real, capturas de pantalla en
                            // dos dispositivos distintos) -- MapLibre no
                            // tolera bien un padding de cámara tan grande
                            // respecto al alto de pantalla. Se deja el valor
                            // fijo original; el ajuste de la barra de
                            // navegación queda sólo en los controles de UI
                            // (brújula, FAB) que no tocan la cámara.
                            puck.paddingWhileTracking(doubleArrayOf(0.0, 0.0, 0.0, 286.dp.toPx().toDouble()))
                        }
                    }
                } else {
                    if (puck.isLocationComponentActivated && puck.cameraMode != CameraMode.NONE) {
                        puck.cameraMode = CameraMode.NONE
                        // Al soltar el seguimiento se vuelve al encuadre de la ruta.
                        encuadreAplicado[0] = null
                    }
                    if (encuadreAplicado[0] != clave) {
                        encuadreAplicado[0] = clave
                        val ubicaciones = ruta.paradas.map { LatLng(it.punto.lat, it.punto.lng) }
                        // Pedido del usuario: se lee de izquierda a derecha,
                        // como texto -- parada 1 a la izquierda, la última a
                        // la derecha. Sin dirección clara (menos de 2
                        // paradas) se mantiene el bearing actual de la cámara.
                        val bearing = bearingLecturaIzqADer(ruta) ?: mapa.cameraPosition.bearing
                        if (ubicaciones.size == 1) {
                            mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicaciones.single(), 15.0))
                        } else if (ubicaciones.size > 1) with(density) {
                            val bounds = LatLngBounds.Builder()
                            ubicaciones.forEach(bounds::include)
                            // Hallazgo real (capturas en Pixel 4a y emulador,
                            // ANDROID-PLAN-04): sumar alturaBarraNav al
                            // padding inferior acá hacía que newLatLngBounds
                            // encuadrara un lugar TOTALMENTE distinto (mitad
                            // de pantalla en blanco, calles de otra zona de
                            // Lima) -- y de forma distinta en cada
                            // dispositivo. MapLibre no tolera bien un padding
                            // de cámara tan grande relativo al alto de
                            // pantalla; se deja el valor fijo original.
                            mapa.moveCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    bounds.build(),
                                    bearing, 0.0,
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
                    .padding(end = 14.dp, bottom = 276.dp + alturaBarraNav),
            )
        }
    }
}

/**
 * Agrega una capa nuestra justo debajo del camión, nunca al revés.
 *
 * Hallazgo real (CRASH confirmado en emulador y Pixel 4a,
 * CannotAddLayerException "Could not find layer: paradas-orden"): forzar
 * al LocationComponent por ENCIMA de una capa nuestra vía
 * `.layerAbove("paradas-orden")` en sus opciones revienta en cualquier
 * recarga de estilo posterior a la primera activación -- MapLibre
 * reinicializa el puck internamente ANTES de que este callback vuelva a
 * agregar "paradas-orden" al estilo nuevo (confirmado en el código fuente
 * de MapLibre 11.8.8, `MapLibreMap.notifyStyleLoaded()`).
 *
 * La vía segura es la inversa: anclar NUESTRAS capas contra el nombre
 * ESTABLE de la capa más baja del propio puck
 * (`LocationComponentConstants.SHADOW_LAYER`). En la primera carga esa
 * capa todavía no existe (el puck ni se activó) -- se cae a `addLayer`
 * normal, y como `activarPuck()` corre al final de este mismo callback,
 * el puck igual termina arriba. En cualquier recarga posterior, el puck
 * ya se reinicializó solo antes de que este callback corra, así que
 * SHADOW_LAYER ya existe y podemos anclar ahí sin riesgo.
 */
private fun agregarDebajoDelCamion(estilo: Style, capa: Layer) {
    if (estilo.getLayer(LocationComponentConstants.SHADOW_LAYER) != null) {
        estilo.addLayerBelow(capa, LocationComponentConstants.SHADOW_LAYER)
    } else {
        estilo.addLayer(capa)
    }
}

// bbox real con el que se extrajo app/src/main/assets/mapa/lima-sur.pmtiles
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
            // Hallazgo real (capturas en emulador): con specialized layer en
            // true, el ícono custom (foregroundDrawable/gpsDrawable) no se
            // renderiza -- MapLibre cae al puck azul default (issue conocido,
            // github.com/maplibre/maplibre-native/issues/2216). En false el
            // camión sí se dibuja.
            .useSpecializedLocationLayer(false)
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
                    // Pedido del usuario: sólo el ícono del camión, sin el
                    // círculo blanco default detrás -- se transparenta el
                    // fondo (normal y "stale") y se saca la sombra.
                    .backgroundTintColor(android.graphics.Color.TRANSPARENT)
                    .backgroundStaleTintColor(android.graphics.Color.TRANSPARENT)
                    .elevation(0f)
                    // Un poco más grande que el default, pero 1.5 quedó
                    // demasiado grande (feedback del usuario) -- 1.15.
                    .minZoomIconScale(1.15f)
                    .maxZoomIconScale(1.15f)
                    // NO layerAbove("paradas-orden") -- hallazgo real
                    // (CRASH en los 2 dispositivos, CannotAddLayerException
                    // "Could not find layer: paradas-orden"): LocationComponent
                    // de MapLibre reregistra un listener interno propio de
                    // "estilo recién cargado" la PRIMERA vez que se activa, y
                    // ese listener interno se dispara en cada recarga de
                    // estilo posterior (con el mapa híbrido, la recarga
                    // offline->online pasa sola, no sólo al cambiar de tema)
                    // ANTES de que nuestro propio callback re-agregue
                    // "paradas-orden" al estilo nuevo -- layerAbove apuntando
                    // a una capa NUESTRA se cae en esa ventana de carrera. El
                    // círculo de una parada tapando el camión (si coinciden
                    // en la misma posición) queda como deuda cosmética menor
                    // -- preferible a un crash.
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
