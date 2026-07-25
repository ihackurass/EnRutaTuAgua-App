package pe.enrutatuagua.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.enrutatuagua.data.local.EnRutaTuAguaDb
import pe.enrutatuagua.domain.modelo.*
import pe.enrutatuagua.domain.repositorio.RutaRepository
import java.time.LocalDate
import javax.inject.Inject

class RutaRepositoryRoom @Inject constructor(private val db: EnRutaTuAguaDb) : RutaRepository {

    override fun observarRutaAbierta(): Flow<Ruta?> =
        db.rutaDao().observarRutaAbierta().map { it?.aDominio() }

    override fun observarEntregas(rutaId: Long): Flow<List<Entrega>> =
        db.entregaDao().observarPorRuta(rutaId).map { lista -> lista.map { it.aDominio() } }

    // Transacción atómica (fix de auditoría): entrega + estado de parada juntos o nada
    override suspend fun registrarEntrega(entrega: Entrega) = db.withTransaction {
        db.entregaDao().insertar(entrega.aEntity())
        db.rutaDao().actualizarParada(entrega.paradaId, EstadoEntrega.ENTREGADO.codigo, null, null)
    }

    override suspend fun omitirParada(paradaId: Long, motivo: MotivoOmision, comentario: String?) {
        db.rutaDao().actualizarParada(paradaId, EstadoEntrega.OMITIDO.codigo, motivo.codigo, comentario)
    }

    override suspend fun sembrarSiVacio() = db.withTransaction {
        if (db.rutaDao().contarRutas() == 0) {
            val (ruta, puntos, paradas) = SeedInicial.ruta(LocalDate.now().toString())
            db.rutaDao().insertarPuntos(puntos)
            db.rutaDao().insertarRuta(ruta)
            db.rutaDao().insertarParadas(paradas)
        }
    }
}
