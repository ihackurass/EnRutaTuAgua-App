package pe.enrutatuagua.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PosicionGpsDao {
    @Insert suspend fun insertar(p: PosicionGpsPendienteEntity): Long

    @Query("SELECT * FROM posicion_gps_pendiente ORDER BY id ASC LIMIT :maxItems")
    suspend fun pendientes(maxItems: Int): List<PosicionGpsPendienteEntity>

    @Query("DELETE FROM posicion_gps_pendiente WHERE id IN (:ids)")
    suspend fun eliminar(ids: List<Long>)

    @Query("DELETE FROM posicion_gps_pendiente WHERE registradaEnMillis < :limiteMillis")
    suspend fun purgarAntiguas(limiteMillis: Long)

    @Query(
        "DELETE FROM posicion_gps_pendiente WHERE id NOT IN " +
            "(SELECT id FROM posicion_gps_pendiente ORDER BY id DESC LIMIT :maxFilas)"
    )
    suspend fun recortarA(maxFilas: Int)
}
