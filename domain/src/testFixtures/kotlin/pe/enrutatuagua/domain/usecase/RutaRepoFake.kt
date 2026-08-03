package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import pe.enrutatuagua.domain.modelo.*
import pe.enrutatuagua.domain.repositorio.RutaRepository
import java.time.LocalDate

class RutaRepoFake : RutaRepository {
    val rutaFlow = MutableStateFlow<Ruta?>(rutaDePrueba())
    val entregasFlow = MutableStateFlow<List<Entrega>>(emptyList())
    val registradas = mutableListOf<Entrega>()
    var omitida: Triple<Long, MotivoOmision, String?>? = null
    var lanzarEnObservarRutaAbierta: Exception? = null

    override fun observarRutaAbierta(): Flow<Ruta?> = flow {
        lanzarEnObservarRutaAbierta?.let { lanzarEnObservarRutaAbierta = null; throw it }
        emitAll(rutaFlow)
    }
    override fun observarEntregas(rutaId: Long): Flow<List<Entrega>> = entregasFlow
    override suspend fun registrarEntrega(entrega: Entrega) {
        lanzarEnRegistrarEntrega?.let { throw it }
        registradas += entrega; entregasFlow.value += entrega
    }
    override suspend fun omitirParada(paradaId: Long, motivo: MotivoOmision, comentario: String?) {
        lanzarEnOmitir?.let { throw it }
        omitida = Triple(paradaId, motivo, comentario)
    }
    override suspend fun sembrarSiVacio() {}

    var lanzarEnAbrir: Exception? = null
    var lanzarEnCerrar: Exception? = null
    var lanzarEnRegistrarEntrega: Exception? = null
    var lanzarEnOmitir: Exception? = null

    override suspend fun abrirRuta(): Ruta {
        lanzarEnAbrir?.let { throw it }
        val ruta = rutaDePrueba()
        rutaFlow.value = ruta
        return ruta
    }

    override suspend fun cerrarRuta(): Ruta {
        lanzarEnCerrar?.let { throw it }
        val cerrada = rutaFlow.value!!.copy(estado = EstadoRuta.CERRADA)
        rutaFlow.value = null // "abierta" sólo representa la ruta ABIERTA, ver RutaRepositoryHttp
        return cerrada
    }

    companion object {
        fun rutaDePrueba(): Ruta {
            val punto = Punto(1, "Comité 12 — Loma Alta", -12.1571, -76.9442, 4000)
            val parada = Parada(10, punto, 1, 4000, EstadoEntrega.PENDIENTE, null, null)
            return Ruta(100, LocalDate.parse("2026-07-25"), EstadoRuta.ABIERTA,
                Vehiculo(5, "ABC-123", 12000), listOf(parada))
        }
    }
}
