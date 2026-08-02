package pe.enrutatuagua.app.ui.mapa

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import pe.enrutatuagua.domain.modelo.Ruta

/**
 * Clave estable del encuadre de cámara: solo la geometría de las paradas.
 * La cámara re-encuadra únicamente cuando cambia esta clave — conceder el
 * permiso de ubicación, entregar una parada o alternar el tema no deben
 * resetear el paneo del usuario.
 */
fun claveDeEncuadre(ruta: Ruta): String =
    ruta.paradas.joinToString("|") { "${it.punto.lat},${it.punto.lng}" }

/**
 * Bearing del mapa (grados, sentido horario desde el norte) para que la
 * ruta se lea de izquierda a derecha en pantalla -- parada #1 (por
 * `orden`, no por posición en la lista) a la izquierda, la última a la
 * derecha, como leer texto. `null` con menos de 2 paradas (no hay
 * dirección que calcular; se mantiene el bearing actual de la cámara).
 */
fun bearingLecturaIzqADer(ruta: Ruta): Double? {
    val paradas = ruta.paradas.sortedBy { it.orden }
    if (paradas.size < 2) return null
    val desde = paradas.first().punto
    val hasta = paradas.last().punto

    val lat1 = Math.toRadians(desde.lat)
    val lat2 = Math.toRadians(hasta.lat)
    val deltaLng = Math.toRadians(hasta.lng - desde.lng)
    val y = sin(deltaLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng)
    val bearingGeografico = (Math.toDegrees(atan2(y, x)) + 360) % 360

    // Esa dirección tiene que aparecer a la DERECHA de la pantalla (no
    // arriba, que sería el bearing crudo) -- de ahí los -90°.
    return (bearingGeografico - 90 + 360) % 360
}
