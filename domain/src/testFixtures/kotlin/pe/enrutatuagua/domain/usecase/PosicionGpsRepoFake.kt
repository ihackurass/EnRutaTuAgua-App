package pe.enrutatuagua.domain.usecase

import pe.enrutatuagua.domain.repositorio.PosicionGps
import pe.enrutatuagua.domain.repositorio.PosicionGpsRepository

class PosicionGpsRepoFake : PosicionGpsRepository {
    val guardadas = mutableListOf<PosicionGps>()
    private var siguienteId = 1L

    override suspend fun guardar(posicion: PosicionGps): PosicionGps {
        val conId = posicion.copy(id = siguienteId++)
        guardadas += conId
        return conId
    }

    var lanzarUnaVezEnPendientes: Exception? = null

    override suspend fun pendientes(maxItems: Int): List<PosicionGps> {
        lanzarUnaVezEnPendientes?.let { lanzarUnaVezEnPendientes = null; throw it }
        return guardadas.sortedBy { it.id }.take(maxItems)
    }

    override suspend fun marcarPublicadas(ids: List<Long>) {
        guardadas.removeAll { it.id in ids }
    }

    override suspend fun purgarAntiguas(limiteMillis: Long, maxFilas: Int) {
        guardadas.removeAll { it.registradaEnMillis < limiteMillis }
        while (guardadas.size > maxFilas) guardadas.removeAt(0)
    }
}
