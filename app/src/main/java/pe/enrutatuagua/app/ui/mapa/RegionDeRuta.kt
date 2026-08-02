package pe.enrutatuagua.app.ui.mapa

import pe.enrutatuagua.domain.modelo.Ruta
import pe.enrutatuagua.domain.repositorio.RegionMapa

/**
 * Bbox de las paradas de la ruta + margen fijo (para precarga de tiles,
 * plan mapa híbrido) -- ~0.03° ≈ 3km a esta latitud, suficiente para que un
 * desvío corto del trazo recto entre paradas siga cubierto. `null` si la
 * ruta no tiene paradas (nada que precargar).
 */
fun regionDeRuta(ruta: Ruta, margenGrados: Double = 0.03): RegionMapa? {
    if (ruta.paradas.isEmpty()) return null
    val lats = ruta.paradas.map { it.punto.lat }
    val lngs = ruta.paradas.map { it.punto.lng }
    return RegionMapa(
        latNorte = lats.max() + margenGrados,
        lonEste = lngs.max() + margenGrados,
        latSur = lats.min() - margenGrados,
        lonOeste = lngs.min() - margenGrados,
    )
}
