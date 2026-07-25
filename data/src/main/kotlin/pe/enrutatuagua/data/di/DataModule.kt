package pe.enrutatuagua.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pe.enrutatuagua.data.RutaRepositoryRoom
import pe.enrutatuagua.data.SesionRepositoryMemoria
import pe.enrutatuagua.data.local.EnRutaTuAguaDb
import pe.enrutatuagua.domain.repositorio.RutaRepository
import pe.enrutatuagua.domain.repositorio.SesionRepository
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object DbModule {
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): EnRutaTuAguaDb =
        Room.databaseBuilder(ctx, EnRutaTuAguaDb::class.java, "enrutatuagua.db").build()
}

@Module @InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {
    @Binds abstract fun rutaRepository(impl: RutaRepositoryRoom): RutaRepository
    @Binds abstract fun sesionRepository(impl: SesionRepositoryMemoria): SesionRepository
}
