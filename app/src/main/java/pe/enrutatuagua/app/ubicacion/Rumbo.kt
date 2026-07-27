package pe.enrutatuagua.app.ubicacion

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Azimut inicial (rumbo ortodrómico) de A hacia B en grados [0, 360),
 * con 0 = norte y 90 = este. Es matemática de presentación del mapa:
 * el puck del camión rota con este ángulo cuando el fix de GPS no trae bearing.
 */
fun rumboEntre(latA: Double, lngA: Double, latB: Double, lngB: Double): Float {
    val fi1 = Math.toRadians(latA)
    val fi2 = Math.toRadians(latB)
    val deltaLambda = Math.toRadians(lngB - lngA)
    val y = sin(deltaLambda) * cos(fi2)
    val x = cos(fi1) * sin(fi2) - sin(fi1) * cos(fi2) * cos(deltaLambda)
    val grados = Math.toDegrees(atan2(y, x))
    return (((grados % 360.0) + 360.0) % 360.0).toFloat()
}
