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
import pe.enrutatuagua.data.red.OmitirParadaRequestDto
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
import retrofit2.HttpException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ANDROID-PLAN-02 (T5): reemplaza a `RutaRepositoryRoom` -- sin cola
 * offline (llamadas directas, best-effort, ver "Fuera de alcance" del
 * plan). `Ruta.vehiculoId`/`Parada.puntoId` viajan planos desde el backend
 * (sin puerto que resuelva objetos anidados, ver `RutaController` del
 * backend) -- este repo los enriquece en paralelo con `PuntosApi`/
 * `VehiculosApi` antes de exponer el `Ruta` de dominio.
 */
@Singleton
class RutaRepositoryHttp @Inject constructor(
    private val rutasApi: RutasApi,
    private val puntosApi: PuntosApi,
    private val vehiculosApi: VehiculosApi,
) : RutaRepository {

    private val estado = MutableStateFlow<Ruta?>(null)
    private val mutex = Mutex()
    @Volatile private var cargada = false

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
        marcarParadaLocalmente(entrega.paradaId, EstadoEntrega.ENTREGADO, motivo = null, comentario = null)
    }

    override suspend fun omitirParada(paradaId: Long, motivo: MotivoOmision, comentario: String?) {
        val rutaId = checkNotNull(estado.value) { "omitirParada sin ruta cargada" }.id
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

    private suspend fun cargarInicial() {
        try {
            refrescar()
        } catch (e: HttpException) {
            if (e.code() == 404) estado.value = null else throw e
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

    private suspend fun enriquecer(dto: RutaDto): Ruta = coroutineScope {
        val vehiculoDeferido = async { vehiculosApi.porId(dto.vehiculoId).aDominio() }
        val puntosDeferidos: Map<Long, Deferred<Punto>> =
            dto.paradas.map { it.puntoId }.distinct().associateWith { id -> async { puntosApi.porId(id).aDominio() } }
        val vehiculo: Vehiculo = vehiculoDeferido.await()
        val puntos = puntosDeferidos.mapValues { it.value.await() }
        dto.aDominio(vehiculo, puntos)
    }
}
