package pe.enrutatuagua.app.ui.mapa

import android.animation.ValueAnimator
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.MovimientoAgua
import pe.enrutatuagua.domain.modelo.Ruta

/**
 * Mapa como lienzo (plan 07 T5): estilo Protomaps offline con los tokens del
 * design system, ruta con halo, pines por estado y latido en la próxima parada.
 * Cámara con padding asimétrico para que la hoja (262) y la columna no tapen la ruta.
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

    DisposableEffect(Unit) {
        onDispose {
            latido.cancel()
            mapView.onPause(); mapView.onStop(); mapView.onDestroy()
        }
    }

    AndroidView(modifier = modifier, factory = { mapView }, update = { vista ->
        vista.getMapAsync { mapa ->
            if (oscuroAplicado[0] != oscuro) {
                oscuroAplicado[0] = oscuro
                mapa.setStyle(Style.Builder().fromJson(EstiloMapa.json(contexto, oscuro))) { estilo ->
                    estilo.addSource(GeoJsonSource("ruta", geoJsonDeRuta(ruta)))
                    estilo.addLayer(
                        LineLayer("ruta-halo", "ruta").withProperties(
                            lineColor(cAccion), lineOpacity(0.22f), lineWidth(13f),
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
                }
            } else {
                (mapa.style?.getSource("ruta") as? GeoJsonSource)?.setGeoJson(geoJsonDeRuta(ruta))
            }

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
    })
}
