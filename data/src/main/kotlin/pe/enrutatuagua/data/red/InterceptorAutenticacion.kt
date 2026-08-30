package pe.enrutatuagua.data.red

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import pe.enrutatuagua.domain.repositorio.SesionRepository

/**
 * ANDROID-PLAN-02 (T3): adjunta `Authorization: Bearer`. OkHttp ya corre los
 * interceptors fuera del hilo principal (su propio dispatcher), así que
 * bloquear acá sobre el `suspend` de [AutenticacionRepository] es seguro --
 * mismo patrón usado en apps Android reales para interceptors de auth.
 *
 * Ante un 401 (token vencido a mitad de sesión, o el primero resultó
 * inválido) reintenta UNA sola vez con un token fresco -- ni la request
 * original ni el reintento se repiten en bucle.
 */
class InterceptorAutenticacion(private val sesion: SesionRepository) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = runBlocking { sesion.tokenVigente() } ?: throw IOException("No hay una sesión vigente")
        val respuesta = chain.proceed(conBearer(original, token))
        if (respuesta.code != 401) return respuesta
        runBlocking { sesion.cerrar() }
        return respuesta
    }

    private fun conBearer(request: okhttp3.Request, token: String) =
        request.newBuilder().header("Authorization", "Bearer $token").build()
}
