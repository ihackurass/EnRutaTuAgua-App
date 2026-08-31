package pe.enrutatuagua.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pe.enrutatuagua.data.ConectividadRepositoryAndroid
import pe.enrutatuagua.data.ContextoTrackingRepositoryRuntime
import pe.enrutatuagua.data.PedidoRepositoryHttp
import pe.enrutatuagua.data.PosicionGpsRepositoryRoom
import pe.enrutatuagua.data.RutaRepositoryHttp
import pe.enrutatuagua.data.SesionRepositorySegura
import pe.enrutatuagua.data.TrackingRepositoryHttp
import pe.enrutatuagua.data.local.EnRutaTuAguaDb
import pe.enrutatuagua.data.mqtt.PublicadorPosicionMqttPaho
import pe.enrutatuagua.domain.repositorio.ConectividadRepository
import pe.enrutatuagua.domain.repositorio.ContextoTrackingRepository
import pe.enrutatuagua.domain.repositorio.PedidoRepository
import pe.enrutatuagua.domain.repositorio.OperacionRepository
import pe.enrutatuagua.data.OperacionRepositoryHttp
import pe.enrutatuagua.domain.repositorio.PosicionGpsRepository
import pe.enrutatuagua.domain.repositorio.PublicadorPosicionMqtt
import pe.enrutatuagua.domain.repositorio.RutaRepository
import pe.enrutatuagua.domain.repositorio.SesionRepository
import pe.enrutatuagua.domain.repositorio.TrackingRepository
import javax.inject.Singleton

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `accion_operativa_pendiente` (`id` TEXT NOT NULL, `tipo` TEXT NOT NULL, `rutaId` INTEGER NOT NULL, `paradaId` INTEGER NOT NULL, `tipoEntrega` TEXT, `ajustaA` TEXT, `litros` INTEGER, `gpsLat` REAL, `gpsLng` REAL, `registradaEnMillis` INTEGER, `motivoOmision` TEXT, `comentario` TEXT, `estado` TEXT NOT NULL, `error` TEXT, `intentos` INTEGER NOT NULL, `creadaEnMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
        )
    }
}

@Module @InstallIn(SingletonComponent::class)
object DbModule {
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): EnRutaTuAguaDb =
        // La migración 4 -> 5 preserva tanto el buffer GPS como las acciones
        // operativas pendientes. El fallback sólo cubre versiones históricas
        // de desarrollo para las que nunca se publicó una migración incremental.
        Room.databaseBuilder(ctx, EnRutaTuAguaDb::class.java, "enrutatuagua.db")
            .addMigrations(MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
}

@Module @InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {
    @Binds abstract fun rutaRepository(impl: RutaRepositoryHttp): RutaRepository
    @Binds abstract fun sesionRepository(impl: SesionRepositorySegura): SesionRepository
    @Binds abstract fun pedidoRepository(impl: PedidoRepositoryHttp): PedidoRepository
    @Binds abstract fun operacionRepository(impl: OperacionRepositoryHttp): OperacionRepository
    @Binds abstract fun trackingRepository(impl: TrackingRepositoryHttp): TrackingRepository
    @Binds abstract fun posicionGpsRepository(impl: PosicionGpsRepositoryRoom): PosicionGpsRepository
    @Binds abstract fun publicadorPosicionMqtt(impl: PublicadorPosicionMqttPaho): PublicadorPosicionMqtt
    @Binds abstract fun conectividadRepository(impl: ConectividadRepositoryAndroid): ConectividadRepository
    @Binds abstract fun contextoTrackingRepository(impl: ContextoTrackingRepositoryRuntime): ContextoTrackingRepository
}
