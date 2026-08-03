package pe.enrutatuagua.app.servicio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import pe.enrutatuagua.app.ui.mapa.claveDeEncuadre
import pe.enrutatuagua.app.ui.mapa.regionDeRuta
import pe.enrutatuagua.domain.modelo.EstadoRuta
import pe.enrutatuagua.domain.repositorio.ConectividadRepository
import pe.enrutatuagua.domain.repositorio.DescargadorRegionMapa
import pe.enrutatuagua.domain.repositorio.RegionMapa
import pe.enrutatuagua.domain.repositorio.RutaRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispara la precarga de tiles de la zona de la ruta abierta mientras hay
 * conectividad (mapa híbrido online/offline) -- best-effort, no bloqueante,
 * mismo espíritu que el resto del rastreo GPS. Espejo estructural de
 * ObservadorRutaAbiertaGps: se dispara por ruta ABIERTA, pero acá además
 * exige conectividad y deduplica por claveDeEncuadre (no por igualdad de
 * Ruta completa) -- una entrega registrada no debe re-disparar una
 * descarga de una zona geográficamente idéntica.
 */
@Singleton
class PrecacheRutaObservador @Inject constructor(
    private val rutaRepository: RutaRepository,
    private val conectividadRepository: ConectividadRepository,
    private val descargadorRegionMapa: DescargadorRegionMapa,
) {
    fun iniciar(alcance: CoroutineScope) {
        combine(rutaRepository.observarRutaAbierta(), conectividadRepository.observarConectividad()) { ruta, online ->
            if (ruta?.estado != EstadoRuta.ABIERTA || !online) return@combine null
            val region = regionDeRuta(ruta) ?: return@combine null
            Trigger(clave = "ruta-${ruta.id}-${claveDeEncuadre(ruta)}", region = region)
        }
            .distinctUntilChanged()
            .onEach { it?.let { trigger -> descargadorRegionMapa.descargar(trigger.clave, trigger.region) } }
            // Mismo hallazgo que ObservadorRutaAbiertaGps: sin retry, un
            // corte de red mata esta colección para siempre y la precarga
            // deja de reaccionar a nuevas rutas el resto de la sesión.
            .retry { delay(5_000L); true }
            .launchIn(alcance)
    }

    private data class Trigger(val clave: String, val region: RegionMapa)
}
