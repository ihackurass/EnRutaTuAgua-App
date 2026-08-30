package pe.enrutatuagua.app.ui.vecino

import org.json.JSONArray
import org.json.JSONObject
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento

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
