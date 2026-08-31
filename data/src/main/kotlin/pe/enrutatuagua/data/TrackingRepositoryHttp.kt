package pe.enrutatuagua.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.data.red.RutasApi
import pe.enrutatuagua.data.red.TrackingApi
import pe.enrutatuagua.data.red.aPosicionCisterna
import pe.enrutatuagua.data.red.aPuntoAbastecimiento
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.modelo.EstadoTracking
import pe.enrutatuagua.domain.modelo.CisternaEnZona
import pe.enrutatuagua.domain.modelo.EstadoValidacionPunto
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.data.local.VecinoCacheDao
import pe.enrutatuagua.data.local.VecinoCisternaCacheEntity
import pe.enrutatuagua.data.local.VecinoPuntoCacheEntity
import pe.enrutatuagua.domain.repositorio.TrackingRepository
import pe.enrutatuagua.domain.repositorio.SesionRepository
import retrofit2.HttpException

private const val INTERVALO_POSICION_MS = 5_000L
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
    private val cliente: OkHttpClient? = null,
    private val sesion: SesionRepository? = null,
    private val baseUrl: String = "",
    private val cacheLocal: VecinoCacheDao? = null,
) : TrackingRepository {

    override suspend fun asociacionActual(): String = trackingApi.asociacionActual().nombre

    // Hallazgo real (verificación manual end-to-end): GET /vehiculos/{id}
    // exige hasRole('AGUATERO') + IDOR contra la última ruta del actor --
    // un Vecino/Presidente SIEMPRE recibía 403 al intentar enriquecer la
    // posición con la capacidad del vehículo por esa vía. /rutas/zona ahora
    // trae vehiculoCapacidadTanqueLitros directo, sin segundo GET.
    private data class ZonaCache(val rutaId: Long, val capacidadTanqueLitros: Int, val resueltaEnMs: Long)

    private var cache: ZonaCache? = null
    private val estadoTracking = MutableStateFlow(EstadoTracking.SIN_POSICION)
    @Volatile private var socket: WebSocket? = null
    @Volatile private var rutaSocket: Long? = null
    @Volatile private var rutasSocket: Set<Long> = emptySet()
    private val json = Json { ignoreUnknownKeys = true }

    override fun observarEstadoTracking(): Flow<EstadoTracking> = estadoTracking

    override fun observarCisternas(): Flow<List<CisternaEnZona>> = callbackFlow {
        val asociacionId = sesion?.sesion?.first()?.asociacionId
        var ultimas = asociacionId?.let { id -> cacheLocal?.cisternas(id)?.map { it.aDominio() } }.orEmpty()
        if (ultimas.isNotEmpty()) trySend(ultimas)
        val polling = launch {
            while (true) {
                if (sesion != null && sesion.rolActual.first() != Rol.VECINO) break
                try {
                    val activas = rutasApi.activasDeZona()
                    val anteriores = ultimas.associateBy { it.rutaId }
                    ultimas = activas.map { ruta ->
                        val zona = ZonaCache(ruta.rutaId, ruta.vehiculoCapacidadTanqueLitros, System.currentTimeMillis())
                        val posicion = posicionDe(zona) ?: anteriores[ruta.rutaId]?.posicion
                        CisternaEnZona(ruta.rutaId, ruta.vehiculoId, ruta.vehiculoCapacidadTanqueLitros, posicion)
                    }
                    val ids = ultimas.map { it.rutaId }.toSet()
                    if (socket == null || rutasSocket != ids) conectarSocketPlural(ultimas) { rutaId, posicion ->
                        ultimas = ultimas.map { if (it.rutaId == rutaId) it.copy(posicion = posicion) else it }
                        estadoTracking.value = EstadoTracking.EN_VIVO
                        trySend(ultimas)
                    }
                    if (estadoTracking.value != EstadoTracking.EN_VIVO) {
                        estadoTracking.value = if (ultimas.any { it.posicion != null }) EstadoTracking.RESPALDO_REST else EstadoTracking.SIN_POSICION
                    }
                    if (asociacionId != null && cacheLocal != null) {
                        cacheLocal.borrarCisternas(asociacionId)
                        cacheLocal.guardarCisternas(ultimas.map { it.aEntity(asociacionId) })
                    }
                    trySend(ultimas)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    estadoTracking.value = EstadoTracking.ERROR
                    trySend(ultimas)
                }
                delay(INTERVALO_POSICION_MS)
            }
        }
        awaitClose { polling.cancel(); cerrarSocket() }
    }

    override fun observarUltimaPosicion(): Flow<PosicionCisterna?> = callbackFlow {
        var ultima: PosicionCisterna? = null
        val polling = launch {
            while (true) {
                if (sesion != null && sesion.rolActual.first() != Rol.VECINO) break
                try {
                    val actual = cache
                    if (actual == null || System.currentTimeMillis() - actual.resueltaEnMs > TTL_RUTA_ZONA_MS) cache = resolverZona()
                    val zona = cache
                    if (zona == null) {
                        cerrarSocket(); estadoTracking.value = EstadoTracking.SIN_POSICION; ultima = null; trySend(null)
                    } else {
                        if (socket == null || rutaSocket != zona.rutaId) conectarSocket(zona) { nueva ->
                            ultima = nueva
                            estadoTracking.value = EstadoTracking.EN_VIVO
                            trySend(nueva)
                        }
                        val posicion = posicionDe(zona)
                        if (posicion != null) ultima = posicion
                        if (estadoTracking.value != EstadoTracking.EN_VIVO) estadoTracking.value = if (ultima == null) EstadoTracking.SIN_POSICION else EstadoTracking.RESPALDO_REST
                        trySend(ultima)
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    estadoTracking.value = EstadoTracking.ERROR
                    // Un corte de red no borra el último dato conocido.
                    trySend(ultima)
                }
                delay(INTERVALO_POSICION_MS)
            }
        }
        awaitClose { polling.cancel(); cerrarSocket() }
    }

    private suspend fun conectarSocket(zona: ZonaCache, entregar: (PosicionCisterna) -> Unit) {
        cerrarSocket()
        val clienteWs = cliente ?: return
        val token = sesion?.tokenVigente() ?: return
        rutaSocket = zona.rutaId
        val wsBase = baseUrl.trimEnd('/').replaceFirst("http://", "ws://").replaceFirst("https://", "wss://")
        socket = clienteWs.newWebSocket(Request.Builder().url("$wsBase/ws/tracking?token=$token").build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\u0000")
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.startsWith("CONNECTED")) {
                    webSocket.send("SUBSCRIBE\nid:posicion-${zona.rutaId}\ndestination:/topic/rutas/${zona.rutaId}/posicion\nack:auto\n\n\u0000")
                } else if (text.startsWith("MESSAGE")) {
                    val cuerpo = text.substringAfter("\n\n").trimEnd('\u0000')
                    runCatching { json.decodeFromString<pe.enrutatuagua.data.red.PosicionVivaDto>(cuerpo) }
                        .map { it.aPosicionCisterna(zona.capacidadTanqueLitros) }.onSuccess(entregar)
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (socket === webSocket) { socket = null; estadoTracking.value = EstadoTracking.RESPALDO_REST }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (socket === webSocket) { socket = null; estadoTracking.value = EstadoTracking.RESPALDO_REST }
            }
        })
    }

    private suspend fun conectarSocketPlural(cisternas: List<CisternaEnZona>, entregar: (Long, PosicionCisterna) -> Unit) {
        cerrarSocket()
        if (cisternas.isEmpty()) return
        val clienteWs = cliente ?: return
        val token = sesion?.tokenVigente() ?: return
        rutasSocket = cisternas.map { it.rutaId }.toSet()
        val capacidades = cisternas.associate { it.rutaId to it.capacidadLitros }
        val wsBase = baseUrl.trimEnd('/').replaceFirst("http://", "ws://").replaceFirst("https://", "wss://")
        socket = clienteWs.newWebSocket(Request.Builder().url("$wsBase/ws/tracking?token=$token").build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\u0000")
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.startsWith("CONNECTED")) {
                    rutasSocket.forEach { rutaId ->
                        webSocket.send("SUBSCRIBE\nid:posicion-$rutaId\ndestination:/topic/rutas/$rutaId/posicion\nack:auto\n\n\u0000")
                    }
                } else if (text.startsWith("MESSAGE")) {
                    val cuerpo = text.substringAfter("\n\n").trimEnd('\u0000')
                    runCatching { json.decodeFromString<pe.enrutatuagua.data.red.PosicionVivaDto>(cuerpo) }
                        .onSuccess { dto -> entregar(dto.rutaId, dto.aPosicionCisterna(capacidades[dto.rutaId] ?: 0)) }
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (socket === webSocket) { socket = null; estadoTracking.value = EstadoTracking.RESPALDO_REST }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (socket === webSocket) { socket = null; estadoTracking.value = EstadoTracking.RESPALDO_REST }
            }
        })
    }

    private fun cerrarSocket() {
        val anterior = socket
        socket = null
        rutaSocket = null
        rutasSocket = emptySet()
        anterior?.close(1000, "fin")
    }

    override fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>> = flow {
        val asociacionId = sesion?.sesion?.first()?.asociacionId
        var ultimos = asociacionId?.let { id -> cacheLocal?.puntos(id)?.map { it.aDominio() } }.orEmpty()
        if (ultimos.isNotEmpty()) emit(ultimos)
        while (true) {
            if (sesion != null && sesion.rolActual.first() != Rol.VECINO) break
            try {
                ultimos = puntosApi.zona().map { it.aPuntoAbastecimiento() }
                if (asociacionId != null && cacheLocal != null) {
                    cacheLocal.borrarPuntos(asociacionId)
                    cacheLocal.guardarPuntos(ultimos.map { it.aEntity(asociacionId) })
                }
                emit(ultimos)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                emit(ultimos)
            }
            delay(INTERVALO_PUNTOS_MS)
        }
    }

    private fun VecinoCisternaCacheEntity.aDominio(): CisternaEnZona = CisternaEnZona(
        rutaId, vehiculoId, capacidadLitros,
        if (lat == null || lng == null || litrosRestantes == null || timestampMs == null) null
        else PosicionCisterna(lat, lng, litrosRestantes, capacidadLitros, timestampMs),
    )

    private fun CisternaEnZona.aEntity(asociacionId: Long) = VecinoCisternaCacheEntity(
        asociacionId, rutaId, vehiculoId, capacidadLitros, posicion?.lat, posicion?.lng,
        posicion?.litrosRestantes, posicion?.timestampMs,
    )

    private fun VecinoPuntoCacheEntity.aDominio() = PuntoAbastecimiento(
        puntoId, nombre, lat, lng, capacidadLitros, EstadoValidacionPunto.valueOf(estado),
    )

    private fun PuntoAbastecimiento.aEntity(asociacionId: Long) = VecinoPuntoCacheEntity(
        asociacionId, id, nombre, lat, lng, capacidadLitros, estado.name,
    )

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
