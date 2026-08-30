package pe.enrutatuagua.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.enrutatuagua.app.BuildConfig
import pe.enrutatuagua.domain.repositorio.ConfiguracionTrackingDev
import javax.inject.Singleton

/**
 * ANDROID-PLAN-04 (alcance acotado, sin login real): valores de
 * configuración de desarrollo para el rastreo GPS, leídos del BuildConfig de
 * :app -- se pisan antes de cada corrida e2e con el aguateroId/rutaId reales
 * del backend sembrado y la IP LAN de la máquina de desarrollo.
 */
@Module
@InstallIn(SingletonComponent::class)
object TrackingDevModule {
    @Provides
    @Singleton
    fun configuracionTrackingDev(): ConfiguracionTrackingDev = ConfiguracionTrackingDev(
        brokerUrl = BuildConfig.MQTT_BROKER_URL_DEV,
    )
}
