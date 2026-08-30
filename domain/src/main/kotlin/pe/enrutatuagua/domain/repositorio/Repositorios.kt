package pe.enrutatuagua.domain.repositorio

import kotlinx.coroutines.flow.Flow
import pe.enrutatuagua.domain.modelo.*

interface RutaRepository {
    fun observarRutaAbierta(): Flow<Ruta?>
    fun observarEntregas(rutaId: Long): Flow<List<Entrega>>
    suspend fun registrarEntrega(entrega: Entrega)
    suspend fun omitirParada(paradaId: Long, motivo: MotivoOmision, comentario: String?)
    suspend fun sembrarSiVacio()

    // ANDROID-PLAN-02 (T5): "hoy salgo a repartir" / cierre de ruta -- el
    // backend es la única autoridad de negocio acá (sin pedidos pendientes,
    // ruta ya abierta, paradas pendientes al cerrar); el cliente no valida
    // nada de eso, sólo propaga el error del servidor.
    suspend fun abrirRuta(): Ruta
    suspend fun cerrarRuta(): Ruta
}

interface SesionRepository {
    val rolActual: Flow<Rol?>
    suspend fun seleccionarRol(rol: Rol)
}

interface PedidoRepository {
    fun observarPedidos(): Flow<List<Pedido>>
    fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>>
    suspend fun crearPedido(puntoId: Long, litros: Int): Pedido
    suspend fun cancelarPedido(pedidoId: Long)
    suspend fun validarPunto(puntoId: Long)
}

interface TrackingRepository {
    fun observarUltimaPosicion(): Flow<PosicionCisterna?>
    fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>>  // RV-02, solo lectura
}

// ANDROID-PLAN-04 (alcance acotado): borde GPS del aguatero, ADR-2. Puerto
// de PERSISTENCIA (RNF-01, "guardado local siempre") separado del puerto de
// RED (best-effort) -- así cada uno se testea con su propio fake, sin
// acoplar "¿guardé el punto?" a si el broker respondió.
data class PosicionGps(
    val id: Long = 0,
    val rutaId: Long,
    val lat: Double,
    val lng: Double,
    val registradaEnMillis: Long,
)

interface PosicionGpsRepository {
    suspend fun guardar(posicion: PosicionGps): PosicionGps
    suspend fun pendientes(maxItems: Int): List<PosicionGps>
    suspend fun marcarPublicadas(ids: List<Long>)
    suspend fun purgarAntiguas(limiteMillis: Long, maxFilas: Int)
}

interface PublicadorPosicionMqtt {
    /** true si el broker confirmó el publish (QoS1) -- no garantiza que el backend lo haya aceptado. */
    suspend fun publicar(aguateroId: Long, posicion: PosicionGps): Boolean
}

/** Valores de configuración de desarrollo (BuildConfig) -- sin login real, ver ANDROID-PLAN-04. */
data class ConfiguracionTrackingDev(
    val brokerUrl: String,
)

data class ContextoTracking(val aguateroId: Long, val rutaId: Long)

interface ContextoTrackingRepository {
    suspend fun actual(): ContextoTracking?
}

// Mapa híbrido online/offline (alcance acotado, ver plan): el PMTiles
// bundleado sigue siendo el piso garantizado sin internet (RNF-01); esto
// es sólo la señal para decidir cuándo preferir el estilo online y cuándo
// disparar la precarga de la zona de la ruta.
interface ConectividadRepository {
    fun observarConectividad(): Flow<Boolean>
}

data class RegionMapa(val latNorte: Double, val lonEste: Double, val latSur: Double, val lonOeste: Double)

interface DescargadorRegionMapa {
    /** Best-effort, sin garantía de completitud (ver "Por qué NO WorkManager" en el plan). */
    fun descargar(clave: String, region: RegionMapa)
}

// ANDROID-PLAN-02 (T2, alcance acotado): sin login real todavía -- ver
// ConfiguracionAuthDev. El token vive SÓLO en memoria (mismo criterio que
// SesionRepositoryMemoria: no sobrevive un relanzamiento del proceso).
interface AutenticacionRepository {
    /** Login perezoso: si no hay token vigente, lo pide antes de devolver. */
    suspend fun tokenVigente(): String

    /** Descarta el token cacheado y vuelve a loguearse -- 401 a mitad de sesión (token vencido server-side). */
    suspend fun forzarRelogin(): String
}

/** Valores de configuración de desarrollo (BuildConfig) -- sin login real, ver ANDROID-PLAN-02. */
data class ConfiguracionAuthDev(val baseUrl: String, val dni: String, val clave: String)
