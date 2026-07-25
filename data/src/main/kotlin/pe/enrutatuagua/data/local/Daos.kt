package pe.enrutatuagua.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class ParadaConPunto(
    @Embedded val parada: ParadaEntity,
    @Relation(parentColumn = "puntoId", entityColumn = "id") val punto: PuntoEntity,
)

data class RutaCompleta(
    @Embedded val ruta: RutaEntity,
    @Relation(entity = ParadaEntity::class, parentColumn = "id", entityColumn = "rutaId")
    val paradas: List<ParadaConPunto>,
)

@Dao
interface RutaDao {
    @Transaction
    @Query("SELECT * FROM ruta WHERE estadoCodigo = 1 ORDER BY id DESC LIMIT 1")
    fun observarRutaAbierta(): Flow<RutaCompleta?>

    @Query("SELECT COUNT(*) FROM ruta") suspend fun contarRutas(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarRuta(r: RutaEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarPuntos(p: List<PuntoEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarParadas(p: List<ParadaEntity>)

    @Query("UPDATE parada SET estadoCodigo = :estado, motivoCodigo = :motivo, comentario = :comentario WHERE id = :paradaId")
    suspend fun actualizarParada(paradaId: Long, estado: Int, motivo: Int?, comentario: String?)
}

@Dao
interface EntregaDao {
    @Query("SELECT e.* FROM entrega e JOIN parada p ON p.id = e.paradaId WHERE p.rutaId = :rutaId")
    fun observarPorRuta(rutaId: Long): Flow<List<EntregaEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertar(e: EntregaEntity)  // idempotente (I-09)
}
