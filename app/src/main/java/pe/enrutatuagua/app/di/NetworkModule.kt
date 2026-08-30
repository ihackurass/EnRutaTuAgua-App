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
import pe.enrutatuagua.data.AutenticacionRepositoryHttp
import pe.enrutatuagua.data.TrackingRepositoryHttp
import pe.enrutatuagua.data.red.AuthApi
import pe.enrutatuagua.data.red.InterceptorAutenticacion
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.data.red.PedidosApi
import pe.enrutatuagua.data.red.RutasApi
import pe.enrutatuagua.data.red.TrackingApi
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
 * Auditoría rol Vecino -- ATAJO DE DESARROLLO, no login real. El rol Vecino
 * no tiene pantalla de login propia todavía (sólo Aguatero, ANDROID-PLAN-02);
 * este calificador autentica siempre como el Vecino ya sembrado por
 * R__seed_desarrollo.sql (dni 10000002) para poder probar
 * TrackingRepositoryHttp end-to-end en un dispositivo real. Cuando exista
 * selección de rol con login real (post-MVP), ESTE CALIFICADOR Y TODO SU
 * STACK (`configuracionVecinoDev`..`puntosApiVecinoDev`) DEBEN
 * ELIMINARSE, y `TrackingRepositoryHttp` debe usar la identidad de la
 * sesión activa -- igual que ya hace el resto de la app para Aguatero.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RetrofitVecinoDev

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
    fun pedidosApi(@RetrofitAutenticado retrofit: Retrofit): PedidosApi = retrofit.create(PedidosApi::class.java)

    @Provides
    @Singleton
    fun vehiculosApi(@RetrofitAutenticado retrofit: Retrofit): VehiculosApi = retrofit.create(VehiculosApi::class.java)

    // --- Identidad dev Vecino (auditoría rol Vecino) -- ver javadoc de RetrofitVecinoDev ---

    @Provides
    @Singleton
    @RetrofitVecinoDev
    fun configuracionVecinoDev(): ConfiguracionAuthDev = ConfiguracionAuthDev(
        baseUrl = BuildConfig.BASE_URL_DEV,
        dni = BuildConfig.VECINO_DNI_DEV,
        clave = BuildConfig.VECINO_CLAVE_DEV,
    )

    @Provides
    @Singleton
    @RetrofitVecinoDev
    fun autenticacionVecinoDev(authApi: AuthApi, @RetrofitVecinoDev configuracion: ConfiguracionAuthDev): AutenticacionRepository =
        AutenticacionRepositoryHttp(authApi, configuracion)

    @Provides
    @Singleton
    @RetrofitVecinoDev
    fun okHttpVecinoDev(@RetrofitVecinoDev autenticacion: AutenticacionRepository): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(InterceptorAutenticacion(autenticacion))
        .build()

    @Provides
    @Singleton
    @RetrofitVecinoDev
    fun retrofitVecinoDev(
        @RetrofitVecinoDev cliente: OkHttpClient,
        @RetrofitVecinoDev configuracion: ConfiguracionAuthDev,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(configuracion.baseUrl)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory(mediaTypeJson))
        .build()

    @Provides
    @Singleton
    @RetrofitVecinoDev
    fun rutasApiVecinoDev(@RetrofitVecinoDev retrofit: Retrofit): RutasApi = retrofit.create(RutasApi::class.java)

    @Provides
    @Singleton
    @RetrofitVecinoDev
    fun puntosApiVecinoDev(@RetrofitVecinoDev retrofit: Retrofit): PuntosApi = retrofit.create(PuntosApi::class.java)

    /** Sin calificador: el mismo endpoint sirve a cualquier actor autorizado -- sólo el Retrofit que lo resuelve está fijo a la identidad Vecino dev. */
    @Provides
    @Singleton
    fun trackingApi(@RetrofitVecinoDev retrofit: Retrofit): TrackingApi = retrofit.create(TrackingApi::class.java)

    /**
     * Hallazgo real (verificación manual end-to-end): `TrackingRepositoryHttp`
     * (`:data`) no puede ver `@RetrofitVecinoDev` (definido acá, en `:app`) --
     * con `@Inject constructor` sin calificar, Dagger le inyectaba en
     * silencio el `RutasApi`/`PuntosApi` SIN calificador (identidad
     * Aguatero), y el backend rechazaba todo con 403 (actor correcto en
     * cuanto a login, pero sin asociacionId). Se construye a mano acá, mismo
     * patrón que `autenticacionVecinoDev`.
     */
    @Provides
    @Singleton
    fun trackingRepositoryHttp(
        @RetrofitVecinoDev rutasApi: RutasApi,
        @RetrofitVecinoDev puntosApi: PuntosApi,
        trackingApi: TrackingApi,
    ): TrackingRepositoryHttp = TrackingRepositoryHttp(rutasApi, puntosApi, trackingApi)
}
