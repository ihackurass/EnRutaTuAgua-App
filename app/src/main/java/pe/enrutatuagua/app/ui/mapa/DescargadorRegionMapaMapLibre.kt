package pe.enrutatuagua.app.ui.mapa

import android.content.Context
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import pe.enrutatuagua.domain.repositorio.DescargadorRegionMapa
import pe.enrutatuagua.domain.repositorio.RegionMapa
import javax.inject.Inject

/**
 * Adaptador real de la precarga (mapa híbrido): usa OfflineManager nativo
 * de MapLibre para bajar y guardar la región en su propio storage offline,
 * contra el estilo ONLINE (para que sirva cuando MapaRuta esté usando ese
 * estilo tras haber tenido señal -- ver "huboConectividadEnEstaRuta" en
 * MapaRuta.kt). Best-effort: sin reintentos ni tracking de progreso, sin
 * bloquear nada si falla (ver plan, "Por qué NO WorkManager").
 */
class DescargadorRegionMapaMapLibre @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : DescargadorRegionMapa {

    override fun descargar(clave: String, region: RegionMapa) {
        // Hallazgo real (crash en emulador, dos veces): PrecacheRutaObservador
        // corre en Dispatchers.Default, pero TODA interacción con MapLibre
        // (incluido MapLibre.getInstance()/OfflineManager, no sólo el MapView)
        // exige el hilo principal -- CalledFromWorkerThreadException si no.
        // Se salta al main thread acá adentro (fire-and-forget, no vale la
        // pena volver `suspend` todo el puerto para esto).
        Handler(Looper.getMainLooper()).post {
            // MapLibre.getInstance() es idempotente -- PrecacheRutaObservador
            // puede disparar esto antes de que MapaRuta (Compose) lo haya
            // inicializado (MapLibreConfigurationException si no).
            MapLibre.getInstance(contexto)
            val bounds = LatLngBounds.from(region.latNorte, region.lonEste, region.latSur, region.lonOeste)
            val styleUrl = "file://${EstiloMapa.prepararArchivoEstiloOnline(contexto, oscuro = false).absolutePath}"
            val definicion = OfflineTilePyramidRegionDefinition(
                styleUrl, bounds, ZOOM_MIN, ZOOM_MAX, contexto.resources.displayMetrics.density,
            )
            OfflineManager.getInstance(contexto).createOfflineRegion(
                definicion,
                clave.toByteArray(),
                object : OfflineManager.CreateOfflineRegionCallback {
                    override fun onCreate(offlineRegion: OfflineRegion) {
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                    }

                    override fun onError(error: String) = Unit
                },
            )
        }
    }

    private companion object {
        const val ZOOM_MIN = 12.0
        const val ZOOM_MAX = 17.0
    }
}
