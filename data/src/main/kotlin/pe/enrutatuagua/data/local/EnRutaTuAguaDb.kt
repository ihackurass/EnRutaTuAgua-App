package pe.enrutatuagua.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PosicionGpsPendienteEntity::class],
    version = 3, exportSchema = false,
)
abstract class EnRutaTuAguaDb : RoomDatabase() {
    abstract fun posicionGpsDao(): PosicionGpsDao
}
