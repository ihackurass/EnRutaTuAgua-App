package pe.enrutatuagua.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.data.di.MIGRATION_4_5
import pe.enrutatuagua.data.di.MIGRATION_5_6
import pe.enrutatuagua.data.local.EnRutaTuAguaDb

@RunWith(RobolectricTestRunner::class)
class MigracionRoomTest {
    @Test
    fun `migrar de 4 a 5 preserva el buffer gps y crea el outbox operativo`() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val nombre = "migracion-4-5.db"
        contexto.deleteDatabase(nombre)
        val archivo = contexto.getDatabasePath(nombre).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(archivo, null).use { anterior ->
            anterior.execSQL(
                """CREATE TABLE IF NOT EXISTS `posicion_gps_pendiente` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mensajeId` TEXT NOT NULL, `rutaId` INTEGER NOT NULL, `lat` REAL NOT NULL, `lng` REAL NOT NULL, `registradaEnMillis` INTEGER NOT NULL)""",
            )
            anterior.execSQL(
                """INSERT INTO posicion_gps_pendiente(mensajeId,rutaId,lat,lng,registradaEnMillis) VALUES('gps-previo',42,-12.1,-77.0,1000)""",
            )
            anterior.version = 4
        }

        val migrada = Room.databaseBuilder(contexto, EnRutaTuAguaDb::class.java, nombre)
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals("gps-previo", migrada.posicionGpsDao().let { dao ->
                kotlinx.coroutines.runBlocking { dao.pendientes(10).single().mensajeId }
            })
            migrada.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='accion_operativa_pendiente'",
            ).use { cursor -> assertTrue(cursor.moveToFirst()) }
        } finally {
            migrada.close()
            contexto.deleteDatabase(nombre)
        }
    }

    @Test
    fun `migrar de 5 a 6 crea la cache de vecino sin perder outboxes`() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val nombre = "migracion-5-6.db"
        contexto.deleteDatabase(nombre)
        val archivo = contexto.getDatabasePath(nombre).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(archivo, null).use { anterior ->
            anterior.execSQL("CREATE TABLE `posicion_gps_pendiente` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mensajeId` TEXT NOT NULL, `rutaId` INTEGER NOT NULL, `lat` REAL NOT NULL, `lng` REAL NOT NULL, `registradaEnMillis` INTEGER NOT NULL)")
            anterior.execSQL("CREATE TABLE `accion_operativa_pendiente` (`id` TEXT NOT NULL PRIMARY KEY, `tipo` TEXT NOT NULL, `rutaId` INTEGER NOT NULL, `paradaId` INTEGER NOT NULL, `tipoEntrega` TEXT, `ajustaA` TEXT, `litros` INTEGER, `gpsLat` REAL, `gpsLng` REAL, `registradaEnMillis` INTEGER, `motivoOmision` TEXT, `comentario` TEXT, `estado` TEXT NOT NULL, `error` TEXT, `intentos` INTEGER NOT NULL, `creadaEnMillis` INTEGER NOT NULL)")
            anterior.version = 5
        }
        val migrada = Room.databaseBuilder(contexto, EnRutaTuAguaDb::class.java, nombre)
            .addMigrations(MIGRATION_5_6).allowMainThreadQueries().build()
        try {
            migrada.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='vecino_cisterna_cache'")
                .use { assertTrue(it.moveToFirst()) }
        } finally { migrada.close(); contexto.deleteDatabase(nombre) }
    }
}
