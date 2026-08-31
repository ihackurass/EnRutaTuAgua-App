package pe.enrutatuagua.domain.repositorio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pe.enrutatuagua.domain.modelo.*

interface RutaRepository {
    fun observarRutaAbierta(): Flow<Ruta?>
    fun invalidarCache()
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
    suspend fun consultarJornada(): SaldoJornada? = null
    suspend fun registrarCargaInicial(mensajeId: String, litros: Int): SaldoJornada = error("Carga no soportada")
    suspend fun registrarRecarga(mensajeId: String, litros: Int): SaldoJornada = error("Recarga no soportada")
}

data class SaldoJornada(
    val id: Long,
    val vehiculoId: Long,
    val rutaId: Long?,
    val capacidadLitros: Int,
    val litrosDisponibles: Int,
)

interface SesionRepository {
    val sesion: Flow<Sesion?>
    val rolActual: Flow<Rol?>
    suspend fun iniciar(dni: String, clave: String): Sesion
    suspend fun seleccionarRol(rol: Rol)
    suspend fun cambiarRol()
    suspend fun cerrar()
    suspend fun tokenVigente(): String?
}

interface PedidoRepository {
    fun observarPedidos(): Flow<List<Pedido>>
    fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>>
    suspend fun crearPedido(puntoId: Long, litros: Int): Pedido
    suspend fun crearPedidoMultipunto(items: List<PedidoItem>): Pedido {
        require(items.size == 1)
        return crearPedido(items.single().puntoId, items.single().litros)
    }
    suspend fun crearPedidoMultipunto(items: List<PedidoItem>, condiciones: CondicionesPedido): Pedido =
        crearPedidoMultipunto(items)
    suspend fun cancelarPedido(pedidoId: Long)
    suspend fun validarPunto(puntoId: Long)
    suspend fun consultarLimite(puntoId: Long): LimitePedido
}

interface OperacionRepository {
    suspend fun resumen(): ResumenOperacion
    suspend fun sugerencia(asociacionId: Long, vehiculoId: Long): SugerenciaRuta
    suspend fun planificar(asociacionId: Long, vehiculoId: Long, aguateroId: Long, fecha: java.time.LocalDate, pedidoIds: List<Long>): RutaPlanificada
    suspend fun listar(fecha: java.time.LocalDate? = null, estado: EstadoRuta? = null): List<RutaOperacion>
    suspend fun cancelar(rutaId: Long): RutaPlanificada
    suspend fun asignada(): RutaPlanificada
    suspend fun iniciar(rutaId: Long): RutaPlanificada
}

interface TrackingRepository {
    fun observarUltimaPosicion(): Flow<PosicionCisterna?>
    fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>>  // RV-02, solo lectura
    suspend fun asociacionActual(): String = "Tu asociación"
    fun observarEstadoTracking(): Flow<EstadoTracking> = flowOf(EstadoTracking.RESPALDO_REST)
}

// ANDROID-PLAN-04 (alcance acotado): borde GPS del aguatero, ADR-2. Puerto
// de PERSISTENCIA (RNF-01, "guardado local siempre") separado del puerto de
// RED (best-effort) -- así cada uno se testea con su propio fake, sin
// acoplar "¿guardé el punto?" a si el broker respondió.
data class PosicionGps(
    val id: Long = 0,
    val mensajeId: String = java.util.UUID.randomUUID().toString(),
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
    val password: String = "",
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
