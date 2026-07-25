package pe.enrutatuagua.app.ui.mapa

import org.json.JSONArray
import org.json.JSONObject
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.modelo.Ruta

fun geoJsonDeRuta(ruta: Ruta): String {
    val proximaId = ruta.paradas.firstOrNull { it.estadoEntrega == EstadoEntrega.PENDIENTE }?.id
    val linea = JSONObject()
        .put("type", "Feature").put("properties", JSONObject())
        .put("geometry", JSONObject().put("type", "LineString")
            .put("coordinates", JSONArray(ruta.paradas.map { JSONArray(listOf(it.punto.lng, it.punto.lat)) })))
    val puntos = ruta.paradas.map { p ->
        JSONObject().put("type", "Feature")
            .put("properties", JSONObject()
                .put("nombre", p.punto.nombre).put("orden", p.orden)
                .put("ordenTxt", p.orden.toString())
                .put("estado", p.estadoEntrega.name)
                .put("proxima", p.id == proximaId))
            .put("geometry", JSONObject().put("type", "Point")
                .put("coordinates", JSONArray(listOf(p.punto.lng, p.punto.lat))))
    }
    return JSONObject().put("type", "FeatureCollection")
        .put("features", JSONArray(listOf(linea) + puntos)).toString()
}
