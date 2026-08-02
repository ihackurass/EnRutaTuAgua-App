package pe.enrutatuagua.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RutaEntity::class, PuntoEntity::class, ParadaEntity::class, EntregaEntity::class,
        PosicionGpsPendienteEntity::class,
    ],
    version = 2, exportSchema = false,
)
abstract class EnRutaTuAguaDb : RoomDatabase() {
    abstract fun rutaDao(): RutaDao
    abstract fun entregaDao(): EntregaDao
    abstract fun posicionGpsDao(): PosicionGpsDao
}
