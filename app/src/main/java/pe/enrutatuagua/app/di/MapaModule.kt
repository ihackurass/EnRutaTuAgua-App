package pe.enrutatuagua.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.enrutatuagua.app.ui.mapa.DescargadorRegionMapaMapLibre
import pe.enrutatuagua.domain.repositorio.DescargadorRegionMapa

@Module
@InstallIn(SingletonComponent::class)
abstract class MapaModule {
    @Binds abstract fun descargadorRegionMapa(impl: DescargadorRegionMapaMapLibre): DescargadorRegionMapa
}
