package pe.enrutatuagua.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pe.enrutatuagua.data.AutenticacionRepositoryHttp
import pe.enrutatuagua.data.ConectividadRepositoryAndroid
import pe.enrutatuagua.data.PedidoRepositoryMemoria
import pe.enrutatuagua.data.PosicionGpsRepositoryRoom
import pe.enrutatuagua.data.RutaRepositoryHttp
import pe.enrutatuagua.data.SesionRepositoryMemoria
import pe.enrutatuagua.data.TrackingRepositoryMemoria
import pe.enrutatuagua.data.local.EnRutaTuAguaDb
import pe.enrutatuagua.data.mqtt.PublicadorPosicionMqttPaho
import pe.enrutatuagua.domain.repositorio.AutenticacionRepository
import pe.enrutatuagua.domain.repositorio.ConectividadRepository
import pe.enrutatuagua.domain.repositorio.PedidoRepository
import pe.enrutatuagua.domain.repositorio.PosicionGpsRepository
import pe.enrutatuagua.domain.repositorio.PublicadorPosicionMqtt
import pe.enrutatuagua.domain.repositorio.RutaRepository
import pe.enrutatuagua.domain.repositorio.SesionRepository
import pe.enrutatuagua.domain.repositorio.TrackingRepository
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object DbModule {
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): EnRutaTuAguaDb =
        // fallbackToDestructiveMigration: sin usuarios reales todavía, no hay
        // migraciones escritas -- el buffer de GPS (v2, ANDROID-PLAN-04) es
        // desechable de por sí, no justifica una migración real.
        Room.databaseBuilder(ctx, EnRutaTuAguaDb::class.java, "enrutatuagua.db")
            .fallbackToDestructiveMigration()
            .build()
}

@Module @InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {
    @Binds abstract fun rutaRepository(impl: RutaRepositoryHttp): RutaRepository
    @Binds abstract fun sesionRepository(impl: SesionRepositoryMemoria): SesionRepository
    @Binds abstract fun pedidoRepository(impl: PedidoRepositoryMemoria): PedidoRepository
    @Binds abstract fun trackingRepository(impl: TrackingRepositoryMemoria): TrackingRepository
    @Binds abstract fun posicionGpsRepository(impl: PosicionGpsRepositoryRoom): PosicionGpsRepository
    @Binds abstract fun publicadorPosicionMqtt(impl: PublicadorPosicionMqttPaho): PublicadorPosicionMqtt
    @Binds abstract fun conectividadRepository(impl: ConectividadRepositoryAndroid): ConectividadRepository
    @Binds abstract fun autenticacionRepository(impl: AutenticacionRepositoryHttp): AutenticacionRepository
}
