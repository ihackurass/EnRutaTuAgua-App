package pe.enrutatuagua.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PosicionGpsPendienteEntity::class, AccionOperativaEntity::class],
    version = 5, exportSchema = false,
)
abstract class EnRutaTuAguaDb : RoomDatabase() {
    abstract fun posicionGpsDao(): PosicionGpsDao
    abstract fun accionOperativaDao(): AccionOperativaDao
}
