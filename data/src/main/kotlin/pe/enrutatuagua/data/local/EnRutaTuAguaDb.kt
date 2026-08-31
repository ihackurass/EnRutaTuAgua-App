package pe.enrutatuagua.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PosicionGpsPendienteEntity::class, AccionOperativaEntity::class, VecinoPuntoCacheEntity::class, VecinoCisternaCacheEntity::class],
    version = 6, exportSchema = false,
)
abstract class EnRutaTuAguaDb : RoomDatabase() {
    abstract fun posicionGpsDao(): PosicionGpsDao
    abstract fun accionOperativaDao(): AccionOperativaDao
    abstract fun vecinoCacheDao(): VecinoCacheDao
}
