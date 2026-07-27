package pe.enrutatuagua.app.ui.mapa

import pe.enrutatuagua.domain.modelo.Ruta

/**
 * Clave estable del encuadre de cámara: solo la geometría de las paradas.
 * La cámara re-encuadra únicamente cuando cambia esta clave — conceder el
 * permiso de ubicación, entregar una parada o alternar el tema no deben
 * resetear el paneo del usuario.
 */
fun claveDeEncuadre(ruta: Ruta): String =
    ruta.paradas.joinToString("|") { "${it.punto.lat},${it.punto.lng}" }
