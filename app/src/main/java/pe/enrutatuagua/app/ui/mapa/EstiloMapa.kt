package pe.enrutatuagua.app.ui.mapa

import android.content.Context
import java.io.File
import org.maplibre.android.geometry.LatLngBounds

/**
 * Estilo Protomaps: por defecto offline (PMTiles empaquetado en assets --
 * Lima Sur, plan 07 §1 -- copiado una sola vez a filesDir), con plantillas
 * JSON claro/oscuro que traen los tokens de mapa del design system. Sin
 * red: tiles y glifos viven en el APK -- esto NUNCA deja de funcionar
 * (RNF-01/RA-06), es el piso garantizado.
 *
 * Mapa híbrido (plan de precarga): `online=true` sustituye la misma marca
 * por una fuente remota en vez del PMTiles local. Es sólo una mejora
 * progresiva encima del modo offline, nunca lo reemplaza.
 *
 * Hallazgo real (emulador, tiles cargaban 200 OK pero el mapa quedaba en
 * blanco): el fallback documentado originalmente en ANDROID-PLAN-07
 * (`tiles.openfreemap.org/planet`) usa el schema OpenMapTiles
 * (`building`, `place`, `landcover`, `transportation`...) -- NO el schema
 * de Protomaps que usan nuestras capas (`buildings`, `places`, `earth`,
 * `roads`...), así que ningún layer encontraba datos aunque las tiles
 * llegaran bien. Se usa en cambio un build diario de Protomaps
 * (build.protomaps.com, la MISMA fuente de la que se extrajo
 * lima-sur.pmtiles) -- mismo schema exacto, cero cambios de estilo. Único
 * costo: la fecha del build hay que refrescarla de tanto en tanto (no hay
 * alias "latest" estable documentado).
 */
object EstiloMapa {
    private const val MARCADOR = "__PMTILES__"
    private const val ASSET_PMTILES = "mapa/lima-sur.pmtiles"
    const val URL_ONLINE = "pmtiles://https://build.protomaps.com/20260801.pmtiles"

    // Bbox real con el que se extrajo lima-sur.pmtiles (ANDROID-PLAN-07 §1)
    // es -76.99,-12.21 a -76.90,-12.12 -- fuera de ese rectángulo no hay
    // tiles a ningún zoom. setLatLngBoundsForCameraTarget sólo frena el
    // CENTRO de la cámara, no el borde visible de la pantalla: se deja un
    // margen de 0.025° (~2.5km) hacia adentro en cada lado para que,
    // combinado con ZOOM_MIN_OFFLINE, el viewport nunca llegue a asomarse al
    // vacío aunque el centro esté pegado al límite permitido. Compartido
    // entre MapaRuta (Aguatero) y MapaZonaVecino -- ambos usan el mismo
    // PMTiles bundleado, así que el mismo límite aplica a los dos.
    val LIMITE_PMTILES: LatLngBounds = LatLngBounds.from(
        latNorth = -12.145, lonEast = -76.925, latSouth = -12.185, lonWest = -76.965,
    )
    const val ZOOM_MIN_OFFLINE = 13.5
    const val ZOOM_MIN_ONLINE = 9.0

    fun prepararPmtiles(contexto: Context): File {
        val destino = File(contexto.filesDir, "mapa/lima-sur.pmtiles")
        if (!destino.exists()) {
            destino.parentFile?.mkdirs()
            contexto.assets.open(ASSET_PMTILES).use { entrada ->
                destino.outputStream().use { salida -> entrada.copyTo(salida) }
            }
        }
        return destino
    }

    fun json(contexto: Context, oscuro: Boolean, online: Boolean = false): String {
        val plantilla = contexto.assets
            .open(if (oscuro) "mapa/estilo-oscuro.json" else "mapa/estilo-claro.json")
            .bufferedReader().use { it.readText() }
        val url = if (online) URL_ONLINE else "pmtiles://file://${prepararPmtiles(contexto).absolutePath}"
        return plantilla.replace(MARCADOR, url)
    }

    /**
     * Materializa el JSON del estilo online a un archivo en filesDir --
     * OfflineTilePyramidRegionDefinition (precarga de tiles) necesita una
     * URL de estilo, no un string en memoria como el que usa `setStyle`.
     */
    fun prepararArchivoEstiloOnline(contexto: Context, oscuro: Boolean): File {
        val destino = File(contexto.filesDir, "mapa/estilo-online-${if (oscuro) "oscuro" else "claro"}.json")
        destino.parentFile?.mkdirs()
        destino.writeText(json(contexto, oscuro, online = true))
        return destino
    }
}
