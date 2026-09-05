package pe.enrutatuagua.app.ui.vecino

import org.json.JSONArray
import org.json.JSONObject
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.modelo.CisternaEnZona

/**
 * GeoJSON de la zona del vecino (RV-01/02): un punto por cada punto de
 * abastecimiento (solo lectura) más, si hay última posición conocida, un
 * feature de cisterna fijo — sin línea de ruta, a diferencia de geoJsonDeRuta.
 */
fun geoJsonDeZona(
    puntos: List<PuntoAbastecimiento>,
    posicion: PosicionCisterna?,
    rumboCisterna: Float? = null,
): String {
    val features = puntos.map { p ->
        JSONObject().put("type", "Feature")
            .put("properties", JSONObject()
                .put("tipo", "punto")
                .put("nombre", p.nombre)
                .put("estado", p.estado.name))
            .put("geometry", JSONObject().put("type", "Point")
                .put("coordinates", JSONArray(listOf(p.lng, p.lat))))
    }
    val conCisterna = if (posicion != null) {
        features + JSONObject().put("type", "Feature")
            .put(
                "properties",
                JSONObject().put("tipo", "cisterna").put("rumbo", rumboCisterna ?: 0f),
            )
            .put("geometry", JSONObject().put("type", "Point")
                .put("coordinates", JSONArray(listOf(posicion.lng, posicion.lat))))
    } else {
        features
    }
    return JSONObject().put("type", "FeatureCollection").put("features", JSONArray(conCisterna)).toString()
}

fun geoJsonDeZonaPlural(
    puntos: List<PuntoAbastecimiento>,
    cisternas: List<CisternaEnZona>,
    rumboCisterna: Float? = null,
    rumbosPorRuta: Map<Long, Float> = emptyMap(),
): String {
    val features = puntos.map { p ->
        JSONObject().put("type", "Feature")
            .put("properties", JSONObject().put("tipo", "punto").put("nombre", p.nombre).put("estado", p.estado.name))
            .put("geometry", JSONObject().put("type", "Point").put("coordinates", JSONArray(listOf(p.lng, p.lat))))
    } + cisternas.mapNotNull { cisterna ->
        cisterna.posicion?.let { posicion ->
            val rumbo = rumbosPorRuta[cisterna.rutaId] ?: rumboCisterna ?: 0f
            JSONObject().put("type", "Feature")
                .put(
                    "properties",
                    JSONObject()
                        .put("tipo", "cisterna")
                        .put("rutaId", cisterna.rutaId)
                        .put("rumbo", rumbo)
                        .put("sprite", nombreSpriteCisterna(rumbo)),
                )
                .put("geometry", JSONObject().put("type", "Point").put("coordinates", JSONArray(listOf(posicion.lng, posicion.lat))))
        }
    }
    return JSONObject().put("type", "FeatureCollection").put("features", JSONArray(features)).toString()
}

/**
 * La hoja generada toma 0° mirando hacia abajo. El rumbo GPS toma 0° hacia
 * el norte, por eso se invierte el ángulo antes de elegir uno de 16 cuadros.
 */
internal fun nombreSpriteCisterna(rumboGps: Float): String {
    val normalizado = ((rumboGps % 360f) + 360f) % 360f
    val anguloHoja = ((180f - normalizado + 360f) % 360f)
    val indice = ((anguloHoja + 11.25f) / 22.5f).toInt() % 16
    return NOMBRES_SPRITE_CISTERNA[indice]
}

private val NOMBRES_SPRITE_CISTERNA = listOf(
    "cisterna-000", "cisterna-022", "cisterna-045", "cisterna-067",
    "cisterna-090", "cisterna-112", "cisterna-135", "cisterna-157",
    "cisterna-180", "cisterna-202", "cisterna-225", "cisterna-247",
    "cisterna-270", "cisterna-292", "cisterna-315", "cisterna-337",
)
