package pe.enrutatuagua.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import pe.enrutatuagua.app.BuildConfig
import pe.enrutatuagua.data.red.AuthApi
import pe.enrutatuagua.data.red.InterceptorAutenticacion
import pe.enrutatuagua.data.red.PedidosApi
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.data.red.RutasApi
import pe.enrutatuagua.data.red.TrackingApi
import pe.enrutatuagua.data.red.OperacionApi
import pe.enrutatuagua.data.red.VehiculosApi
import pe.enrutatuagua.data.TrackingRepositoryHttp
import pe.enrutatuagua.domain.repositorio.SesionRepository
import retrofit2.Retrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RetrofitSinAuth

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RetrofitAutenticado

/** Un solo stack autenticado: la identidad siempre procede de la sesión real. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaTypeJson = "application/json".toMediaType()

    @Provides @Singleton @RetrofitSinAuth
    fun retrofitSinAuth(): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL_DEV)
        .addConverterFactory(json.asConverterFactory(mediaTypeJson))
        .build()

    @Provides @Singleton
    fun authApi(@RetrofitSinAuth retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun okHttpAutenticado(sesion: SesionRepository): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(InterceptorAutenticacion(sesion))
        .build()

    @Provides @Singleton @RetrofitAutenticado
    fun retrofitAutenticado(cliente: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL_DEV)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory(mediaTypeJson))
        .build()

    @Provides @Singleton fun rutasApi(@RetrofitAutenticado r: Retrofit): RutasApi = r.create(RutasApi::class.java)
    @Provides @Singleton fun puntosApi(@RetrofitAutenticado r: Retrofit): PuntosApi = r.create(PuntosApi::class.java)
    @Provides @Singleton fun pedidosApi(@RetrofitAutenticado r: Retrofit): PedidosApi = r.create(PedidosApi::class.java)
    @Provides @Singleton fun vehiculosApi(@RetrofitAutenticado r: Retrofit): VehiculosApi = r.create(VehiculosApi::class.java)
    @Provides @Singleton fun trackingApi(@RetrofitAutenticado r: Retrofit): TrackingApi = r.create(TrackingApi::class.java)
    @Provides @Singleton fun operacionApi(@RetrofitAutenticado r: Retrofit): OperacionApi = r.create(OperacionApi::class.java)
    @Provides @Singleton fun trackingRepositoryHttp(
        rutas: RutasApi, puntos: PuntosApi, tracking: TrackingApi,
        cliente: OkHttpClient, sesion: SesionRepository, cache: pe.enrutatuagua.data.local.VecinoCacheDao,
    ): TrackingRepositoryHttp = TrackingRepositoryHttp(
        rutas, puntos, tracking, cliente, sesion, BuildConfig.BASE_URL_DEV, cache,
    )
}
