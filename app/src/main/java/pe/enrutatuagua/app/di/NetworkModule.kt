package pe.enrutatuagua.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import pe.enrutatuagua.app.BuildConfig
import pe.enrutatuagua.data.red.AuthApi
import pe.enrutatuagua.data.red.InterceptorAutenticacion
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.data.red.RutasApi
import pe.enrutatuagua.data.red.VehiculosApi
import pe.enrutatuagua.domain.repositorio.AutenticacionRepository
import pe.enrutatuagua.domain.repositorio.ConfiguracionAuthDev
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Sin el interceptor de auth -- AuthApi.login lo usa para evitar el ciclo "para loguearse necesito estar logueado". */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RetrofitSinAuth

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RetrofitAutenticado

/**
 * ANDROID-PLAN-02 (T7): BuildConfig vive en :app, así que este módulo
 * también (mismo criterio que TrackingDevModule, plan 04).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaTypeJson = "application/json".toMediaType()

    @Provides
    @Singleton
    fun configuracionAuthDev(): ConfiguracionAuthDev = ConfiguracionAuthDev(
        baseUrl = BuildConfig.BASE_URL_DEV,
        dni = BuildConfig.AGUATERO_DNI_DEV,
        clave = BuildConfig.AGUATERO_CLAVE_DEV,
    )

    @Provides
    @Singleton
    @RetrofitSinAuth
    fun retrofitSinAuth(configuracion: ConfiguracionAuthDev): Retrofit = Retrofit.Builder()
        .baseUrl(configuracion.baseUrl)
        .addConverterFactory(json.asConverterFactory(mediaTypeJson))
        .build()

    @Provides
    @Singleton
    fun authApi(@RetrofitSinAuth retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun okHttpAutenticado(autenticacion: AutenticacionRepository): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(InterceptorAutenticacion(autenticacion))
        .build()

    @Provides
    @Singleton
    @RetrofitAutenticado
    fun retrofitAutenticado(cliente: OkHttpClient, configuracion: ConfiguracionAuthDev): Retrofit = Retrofit.Builder()
        .baseUrl(configuracion.baseUrl)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory(mediaTypeJson))
        .build()

    @Provides
    @Singleton
    fun rutasApi(@RetrofitAutenticado retrofit: Retrofit): RutasApi = retrofit.create(RutasApi::class.java)

    @Provides
    @Singleton
    fun puntosApi(@RetrofitAutenticado retrofit: Retrofit): PuntosApi = retrofit.create(PuntosApi::class.java)

    @Provides
    @Singleton
    fun vehiculosApi(@RetrofitAutenticado retrofit: Retrofit): VehiculosApi = retrofit.create(VehiculosApi::class.java)
}
