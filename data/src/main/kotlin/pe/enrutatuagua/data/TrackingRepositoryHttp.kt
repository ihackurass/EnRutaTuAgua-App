package pe.enrutatuagua.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.data.red.RutasApi
import pe.enrutatuagua.data.red.TrackingApi
import pe.enrutatuagua.data.red.aPosicionCisterna
import pe.enrutatuagua.data.red.aPuntoAbastecimiento
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.repositorio.TrackingRepository
import retrofit2.HttpException

private const val INTERVALO_POSICION_MS = 15_000L // cadencia GPS del aguatero (CapturadorPosicionGps)
private const val INTERVALO_PUNTOS_MS = 120_000L // los puntos de zona cambian poco
private const val TTL_RUTA_ZONA_MS = 60_000L

/**
 * Auditoría rol Vecino: reemplaza a `TrackingRepositoryMemoria` -- sin
 * WebSocket/STOMP (la app no tiene ningún cliente hoy, ver plan), se hace
 * polling sobre el fallback REST ya existente
 * (`GET /tracking/rutas/{rutaId}/posicion`). El `rutaId`/`vehiculoId` de la
 * zona se cachean con un TTL corto en vez de resolverse en cada tick de
 * posición -- ese endpoint no cambia salvo que el aguatero abra/cierre ruta.
 *
 * <p>Sin {@code @Inject constructor}: {@code rutasApi`/`puntosApi` deben ser
 * los calificados {@code @RetrofitVecinoDev} (identidad Vecino), pero ese
 * calificador vive en {@code :app} (di/NetworkModule.kt) y este archivo está
 * en {@code :data} -- no puede verlo (dependencia hacia atrás). Hallazgo
 * real (verificación manual end-to-end): con {@code @Inject constructor} sin
 * calificar, Dagger inyectaba silenciosamente el {@code RutasApi}/
 * {@code PuntosApi} SIN calificador (los de la identidad Aguatero, ya
 * bindeados en otro lado) -- la app pedía todo con el actor equivocado
 * (asociacionId=null) y el backend lo rechazaba con 403, correctamente.
 * Se provee manualmente en {@code NetworkModule.trackingRepositoryHttp},
 * mismo patrón que {@code autenticacionVecinoDev} ahí mismo.
 */
class TrackingRepositoryHttp(
    private val rutasApi: RutasApi,
    private val puntosApi: PuntosApi,
    private val trackingApi: TrackingApi,
) : TrackingRepository {

    // Hallazgo real (verificación manual end-to-end): GET /vehiculos/{id}
    // exige hasRole('AGUATERO') + IDOR contra la última ruta del actor --
    // un Vecino/Presidente SIEMPRE recibía 403 al intentar enriquecer la
    // posición con la capacidad del vehículo por esa vía. /rutas/zona ahora
    // trae vehiculoCapacidadTanqueLitros directo, sin segundo GET.
    private data class ZonaCache(val rutaId: Long, val capacidadTanqueLitros: Int, val resueltaEnMs: Long)

    private var cache: ZonaCache? = null

    override fun observarUltimaPosicion(): Flow<PosicionCisterna?> = flow {
        while (true) {
            val actual = cache
            if (actual == null || System.currentTimeMillis() - actual.resueltaEnMs > TTL_RUTA_ZONA_MS) {
                cache = resolverZona()
            }
            emit(cache?.let { posicionDe(it) })
            delay(INTERVALO_POSICION_MS)
        }
    }

    override fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>> = flow {
        while (true) {
            emit(puntosApi.zona().map { it.aPuntoAbastecimiento() })
            delay(INTERVALO_PUNTOS_MS)
        }
    }

    // Sin ruta ABIERTA en la zona ahora mismo es un estado válido ("no hay
    // cisterna en camino hoy"), no un error -- sólo el 404 se absorbe; el
    // resto se propaga para que el .retry{} del ViewModel decida.
    private suspend fun resolverZona(): ZonaCache? = try {
        val dto = rutasApi.zona()
        ZonaCache(dto.rutaId, dto.vehiculoCapacidadTanqueLitros, System.currentTimeMillis())
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    }

    // Ruta ABIERTA pero sin fix de GPS reciente (TTL vencido del lado
    // tracking) también es 404 -- mismo criterio, estado válido.
    private suspend fun posicionDe(zona: ZonaCache): PosicionCisterna? {
        val posicion = try {
            trackingApi.posicionViva(zona.rutaId)
        } catch (e: HttpException) {
            if (e.code() == 404) return null else throw e
        }
        return posicion.aPosicionCisterna(zona.capacidadTanqueLitros)
    }
}
