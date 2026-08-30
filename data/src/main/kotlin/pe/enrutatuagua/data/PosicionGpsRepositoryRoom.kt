package pe.enrutatuagua.data

import pe.enrutatuagua.data.local.EnRutaTuAguaDb
import pe.enrutatuagua.data.local.PosicionGpsPendienteEntity
import pe.enrutatuagua.domain.repositorio.PosicionGps
import pe.enrutatuagua.domain.repositorio.PosicionGpsRepository
import javax.inject.Inject

fun PosicionGpsPendienteEntity.aDominio() = PosicionGps(id, mensajeId, rutaId, lat, lng, registradaEnMillis)
fun PosicionGps.aEntity() = PosicionGpsPendienteEntity(id, mensajeId, rutaId, lat, lng, registradaEnMillis)

class PosicionGpsRepositoryRoom @Inject constructor(private val db: EnRutaTuAguaDb) : PosicionGpsRepository {

    override suspend fun guardar(posicion: PosicionGps): PosicionGps {
        val id = db.posicionGpsDao().insertar(posicion.aEntity())
        return posicion.copy(id = id)
    }

    override suspend fun pendientes(maxItems: Int): List<PosicionGps> =
        db.posicionGpsDao().pendientes(maxItems).map { it.aDominio() }

    override suspend fun marcarPublicadas(ids: List<Long>) {
        if (ids.isNotEmpty()) db.posicionGpsDao().eliminar(ids)
    }

    override suspend fun purgarAntiguas(limiteMillis: Long, maxFilas: Int) {
        db.posicionGpsDao().purgarAntiguas(limiteMillis)
        db.posicionGpsDao().recortarA(maxFilas)
    }
}
