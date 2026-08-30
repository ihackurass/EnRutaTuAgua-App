package pe.enrutatuagua.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.enrutatuagua.app.ubicacion.ProveedorAnclaTracking
import pe.enrutatuagua.app.ubicacion.ProveedorAnclaTrackingAndroid

@Module
@InstallIn(SingletonComponent::class)
abstract class UbicacionModule {
    @Binds abstract fun proveedorAnclaTracking(impl: ProveedorAnclaTrackingAndroid): ProveedorAnclaTracking
}
