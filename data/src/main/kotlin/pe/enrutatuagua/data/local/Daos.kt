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

@Dao
interface AccionOperativaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertar(accion: AccionOperativaEntity)
    @Query("SELECT * FROM accion_operativa_pendiente WHERE estado='PENDIENTE' ORDER BY creadaEnMillis,id LIMIT :limite")
    suspend fun pendientes(limite: Int): List<AccionOperativaEntity>
    @Query("SELECT * FROM accion_operativa_pendiente WHERE id=:id") suspend fun porId(id: String): AccionOperativaEntity?
    @Query("SELECT * FROM accion_operativa_pendiente ORDER BY creadaEnMillis,id")
    fun observar(): Flow<List<AccionOperativaEntity>>
    @Query("DELETE FROM accion_operativa_pendiente WHERE id IN (:ids)") suspend fun eliminar(ids: List<String>)
    @Query("UPDATE accion_operativa_pendiente SET estado='RECHAZADA',error=:error,intentos=intentos+1 WHERE id=:id")
    suspend fun rechazar(id: String, error: String?)
    @Query("UPDATE accion_operativa_pendiente SET intentos=intentos+1,error=:error WHERE id IN (:ids)")
    suspend fun registrarFallo(ids: List<String>, error: String?)
    @Query("DELETE FROM accion_operativa_pendiente WHERE estado='RECHAZADA'")
    suspend fun eliminarRechazadas()
}

@Dao
interface VecinoCacheDao {
    @Query("SELECT * FROM vecino_punto_cache WHERE asociacionId=:asociacionId ORDER BY puntoId")
    suspend fun puntos(asociacionId: Long): List<VecinoPuntoCacheEntity>
    @Query("SELECT * FROM vecino_cisterna_cache WHERE asociacionId=:asociacionId ORDER BY rutaId")
    suspend fun cisternas(asociacionId: Long): List<VecinoCisternaCacheEntity>
    @Query("DELETE FROM vecino_punto_cache WHERE asociacionId=:asociacionId")
    suspend fun borrarPuntos(asociacionId: Long)
    @Query("DELETE FROM vecino_cisterna_cache WHERE asociacionId=:asociacionId")
    suspend fun borrarCisternas(asociacionId: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun guardarPuntos(items: List<VecinoPuntoCacheEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun guardarCisternas(items: List<VecinoCisternaCacheEntity>)
}
