package pe.enrutatuagua.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.data.red.RutasApi
import pe.enrutatuagua.data.red.TrackingApi
import pe.enrutatuagua.data.red.VehiculosApi
import pe.enrutatuagua.data.red.aPosicionCisterna
import pe.enrutatuagua.data.red.aPuntoAbastecimiento
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.repositorio.TrackingRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

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
 */
@Singleton
class TrackingRepositoryHttp @Inject constructor(
    private val rutasApi: RutasApi,
    private val puntosApi: PuntosApi,
    private val vehiculosApi: VehiculosApi,
    private val trackingApi: TrackingApi,
) : TrackingRepository {

    private data class ZonaCache(val rutaId: Long, val vehiculoId: Long, val resueltaEnMs: Long)

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
        ZonaCache(dto.rutaId, dto.vehiculoId, System.currentTimeMillis())
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
        val vehiculo = vehiculosApi.porId(zona.vehiculoId)
        return posicion.aPosicionCisterna(vehiculo.capacidadTanqueLitros)
    }
}
