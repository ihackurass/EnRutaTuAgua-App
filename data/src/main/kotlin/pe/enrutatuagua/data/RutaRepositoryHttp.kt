package pe.enrutatuagua.data

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import java.io.IOException
import java.util.UUID
import pe.enrutatuagua.data.local.AccionOperativaEntity
import pe.enrutatuagua.data.local.EnRutaTuAguaDb
import pe.enrutatuagua.data.red.SyncRequestDto
import pe.enrutatuagua.data.red.OmisionSyncDto
import pe.enrutatuagua.domain.modelo.EstadoSyncOperativo
import pe.enrutatuagua.domain.repositorio.ConectividadRepository
import pe.enrutatuagua.data.red.OmitirParadaRequestDto
import pe.enrutatuagua.data.red.MovimientoAguaRequestDto
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.data.red.RegistrarEntregaRequestDto
import pe.enrutatuagua.data.red.RutaDto
import pe.enrutatuagua.data.red.RutasApi
import pe.enrutatuagua.data.red.VehiculosApi
import pe.enrutatuagua.data.red.aDominio
import pe.enrutatuagua.data.red.entregasAproximadas
import pe.enrutatuagua.domain.modelo.Entrega
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.modelo.MotivoOmision
import pe.enrutatuagua.domain.modelo.Punto
import pe.enrutatuagua.domain.modelo.Ruta
import pe.enrutatuagua.domain.modelo.Vehiculo
import pe.enrutatuagua.domain.repositorio.RutaRepository
import pe.enrutatuagua.domain.repositorio.SaldoJornada
import retrofit2.HttpException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio HTTP de la ruta con outbox Room para entregas y omisiones.
 * Primero registra la intención local y luego la envía mediante `/sync`,
 * conservando la misma identidad durante todos los reintentos.
 * `Ruta.vehiculoId`/`Parada.puntoId` viajan planos desde el backend
 * (sin puerto que resuelva objetos anidados, ver `RutaController` del
 * backend) -- este repo los enriquece en paralelo con `PuntosApi`/
 * `VehiculosApi` antes de exponer el `Ruta` de dominio.
 */
@Singleton
class RutaRepositoryHttp @Inject constructor(
    private val rutasApi: RutasApi,
    private val puntosApi: PuntosApi,
    private val vehiculosApi: VehiculosApi,
    private val db: EnRutaTuAguaDb? = null,
    private val conectividad: ConectividadRepository? = null,
) : RutaRepository {

    private val estado = MutableStateFlow<Ruta?>(null)
    private val mutex = Mutex()
    private val mutexSync = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var cargada = false

    init {
        if (db != null && conectividad != null) scope.launch {
            conectividad.observarConectividad().collectLatest { if (it) runCatching { sincronizarPendientes() } }
        }
    }

    override fun observarEstadoSync(): Flow<EstadoSyncOperativo> = db?.accionOperativaDao()?.observar()?.map { acciones ->
        EstadoSyncOperativo(acciones.count { it.estado == "PENDIENTE" }, acciones.count { it.estado == "RECHAZADA" },
            acciones.lastOrNull { it.error != null }?.error)
    } ?: kotlinx.coroutines.flow.flowOf(EstadoSyncOperativo())

    override suspend fun reintentarSync() {
        sincronizarPendientes()
    }

    override fun observarRutaAbierta(): Flow<Ruta?> = flow {
        if (!cargada) {
            cargarInicial()
            cargada = true
        }
        emitAll(estado)
    }

    override fun invalidarCache() {
        cargada = false
    }

    override fun observarEntregas(rutaId: Long): Flow<List<Entrega>> =
        observarRutaAbierta().map { it?.entregasAproximadas() ?: emptyList() }

    // Hallazgo real (T8): un GET /rutas/abierta inmediatamente después del
    // POST podía traer el estado ANTERIOR de la parada (visto en pruebas
    // reales contra el backend, con timing exacto reproducible) -- en vez
    // de arriesgar esa carrera, se parchea el caché local con lo que el
    // propio POST ya confirmó (2xx = éxito), sin una vuelta de red extra.
    override suspend fun registrarEntrega(entrega: Entrega) {
        val dao = db?.accionOperativaDao()
        if (dao != null) {
            dao.insertar(AccionOperativaEntity(entrega.id,"ENTREGA",estado.value?.id ?: 0,entrega.paradaId,
                entrega.tipo.name,entrega.ajustaA,entrega.litros,entrega.gpsLat,entrega.gpsLng,entrega.registradaEn))
            aplicarEntregaLocal(entrega.paradaId, entrega.litros)
            procesarNueva(entrega.id)
            return
        }
        rutasApi.registrarEntrega(
            RegistrarEntregaRequestDto(
                id = entrega.id,
                rutaPuntoId = entrega.paradaId,
                tipo = entrega.tipo.name,
                ajustaA = entrega.ajustaA,
                litros = entrega.litros,
                gpsLat = entrega.gpsLat,
                gpsLng = entrega.gpsLng,
                registradaEn = Instant.ofEpochMilli(entrega.registradaEn).toString(),
            ),
        )
        aplicarEntregaLocal(entrega.paradaId, entrega.litros)
    }

    override suspend fun omitirParada(paradaId: Long, motivo: MotivoOmision, comentario: String?) {
        val rutaId = checkNotNull(estado.value) { "omitirParada sin ruta cargada" }.id
        val dao = db?.accionOperativaDao()
        if (dao != null) {
            val id = "omision:$rutaId:$paradaId"
            dao.insertar(
                AccionOperativaEntity(
                    id = id,
                    tipo = "OMISION",
                    rutaId = rutaId,
                    paradaId = paradaId,
                    motivoOmision = motivo.name,
                    comentario = comentario,
                ),
            )
            marcarParadaLocalmente(paradaId, EstadoEntrega.OMITIDO, motivo, comentario)
            procesarNueva(id)
            return
        }
        rutasApi.omitir(rutaId, paradaId, OmitirParadaRequestDto(motivo.name, comentario))
        marcarParadaLocalmente(paradaId, EstadoEntrega.OMITIDO, motivo, comentario)
    }

    override suspend fun sembrarSiVacio() {
        // No-op: el backend ya tiene datos reales sembrados por Flyway.
    }

    override suspend fun abrirRuta(): Ruta = mutex.withLock {
        val ruta = enriquecer(rutasApi.abrir())
        estado.value = ruta
        cargada = true
        ruta
    }

    // Hallazgo real (T8): la respuesta de POST .../cerrar es la ruta YA
    // CERRADA -- si se guardara tal cual en `estado`, la pantalla seguiría
    // mostrando "ruta en curso" (nada filtra por estado). `estado` sólo
    // representa la ruta ABIERTA (mismo contrato que `RutaDao` en Room:
    // `WHERE estadoCodigo = 1`), así que tras cerrar pasa a null.
    override suspend fun cerrarRuta(): Ruta = mutex.withLock {
        val rutaId = checkNotNull(estado.value) { "cerrarRuta sin ruta cargada" }.id
        val cerrada = enriquecer(rutasApi.cerrar(rutaId))
        estado.value = null
        cerrada
    }

    override suspend fun consultarJornada(): SaldoJornada? = try {
        rutasApi.jornada().aDominio()
    } catch (e: HttpException) {
        if (e.code() == 409 || e.code() == 404) null else throw e
    }

    override suspend fun registrarCargaInicial(mensajeId: String, litros: Int) =
        rutasApi.cargaInicial(MovimientoAguaRequestDto(mensajeId, litros)).aDominio()

    override suspend fun registrarRecarga(mensajeId: String, litros: Int) =
        rutasApi.recargar(MovimientoAguaRequestDto(mensajeId, litros)).aDominio()

    private suspend fun cargarInicial() {
        runCatching { sincronizarPendientes() }
        try {
            refrescar()
        } catch (e: HttpException) {
            if (e.code() == 404) estado.value = null else throw e
        }
    }

    private suspend fun procesarNueva(id: String) {
        sincronizarPendientes()
        val accion = db?.accionOperativaDao()?.porId(id)
        if (accion?.estado == "RECHAZADA") {
            runCatching { refrescar() }
            throw IllegalStateException(accion.error ?: "La operación fue rechazada por el servidor")
        }
    }

    private suspend fun sincronizarPendientes() = mutexSync.withLock {
        val dao = db?.accionOperativaDao() ?: return@withLock
        val acciones = dao.pendientes(50)
        if (acciones.isEmpty()) return@withLock
        val entregas = acciones.filter { it.tipo == "ENTREGA" }.map {
            RegistrarEntregaRequestDto(
                it.id, it.paradaId, checkNotNull(it.tipoEntrega), it.ajustaA,
                checkNotNull(it.litros), it.gpsLat, it.gpsLng,
                Instant.ofEpochMilli(checkNotNull(it.registradaEnMillis)).toString(),
            )
        }
        val omisiones = acciones.filter { it.tipo == "OMISION" }.map {
            OmisionSyncDto(it.rutaId, it.paradaId, checkNotNull(it.motivoOmision), it.comentario)
        }
        try {
            val respuesta = rutasApi.sincronizar(SyncRequestDto(entregas, omisiones))
            val aceptadas = mutableListOf<String>()
            respuesta.entregas.forEach { resultado ->
                if (resultado.ok) aceptadas += resultado.id else dao.rechazar(resultado.id, resultado.error)
            }
            respuesta.omisiones.forEach { resultado ->
                val accion = acciones.first { it.tipo == "OMISION" && it.paradaId == resultado.rutaPuntoId }
                if (resultado.ok) aceptadas += accion.id else dao.rechazar(accion.id, resultado.error)
            }
            if (aceptadas.isNotEmpty()) dao.eliminar(aceptadas)
        } catch (e: IOException) {
            dao.registrarFallo(acciones.map { it.id }, e.message)
        }
    }

    private suspend fun refrescar(): Ruta = mutex.withLock {
        val ruta = enriquecer(rutasApi.abierta())
        estado.value = ruta
        ruta
    }

    private fun marcarParadaLocalmente(paradaId: Long, nuevoEstado: EstadoEntrega, motivo: MotivoOmision?, comentario: String?) {
        val actual = estado.value ?: return
        estado.value = actual.copy(
            paradas = actual.paradas.map {
                if (it.id == paradaId) it.copy(estadoEntrega = nuevoEstado, motivoOmision = motivo, comentarioOmision = comentario)
                else it
            },
        )
    }

    private fun aplicarEntregaLocal(paradaId: Long, litros: Int) {
        val actual = estado.value ?: return
        estado.value = actual.copy(paradas = actual.paradas.map { parada ->
            if (parada.id != paradaId) parada else {
                val acumulado = (parada.litrosEntregados + litros).coerceAtMost(parada.capacidadEsperadaLitros)
                parada.copy(
                    litrosEntregados = acumulado,
                    estadoEntrega = if (acumulado == parada.capacidadEsperadaLitros) EstadoEntrega.ENTREGADO else EstadoEntrega.PARCIAL,
                )
            }
        })
    }

    private suspend fun enriquecer(dto: RutaDto): Ruta = coroutineScope {
        val vehiculoDeferido = async { vehiculosApi.porId(dto.vehiculoId).aDominio() }
        val puntosDeferidos: Map<Long, Deferred<Punto>> =
            dto.paradas.map { it.puntoId }.distinct().associateWith { id -> async { puntosApi.porId(id).aDominio() } }
        val vehiculo: Vehiculo = vehiculoDeferido.await()
        val puntos = puntosDeferidos.mapValues { it.value.await() }
        dto.aDominio(vehiculo, puntos)
    }
}

private fun pe.enrutatuagua.data.red.SaldoJornadaDto.aDominio() =
    SaldoJornada(id, vehiculoId, rutaId, capacidadLitros, litrosDisponibles)
