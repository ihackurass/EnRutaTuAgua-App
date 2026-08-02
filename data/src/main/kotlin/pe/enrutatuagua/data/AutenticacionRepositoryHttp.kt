package pe.enrutatuagua.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.enrutatuagua.data.red.AuthApi
import pe.enrutatuagua.data.red.LoginRequestDto
import pe.enrutatuagua.domain.repositorio.AutenticacionRepository
import pe.enrutatuagua.domain.repositorio.ConfiguracionAuthDev
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ANDROID-PLAN-02 (T2): login perezoso con credenciales de desarrollo
 * (BuildConfig, sin pantalla real todavía). Token + expiración en memoria
 * únicamente -- mismo criterio que SesionRepositoryMemoria, no sobrevive un
 * relanzamiento del proceso. El Mutex evita logins duplicados si varias
 * llamadas concurrentes (varios endpoints a la vez) piden token sin uno
 * vigente aún.
 */
@Singleton
class AutenticacionRepositoryHttp @Inject constructor(
    private val api: AuthApi,
    private val configuracion: ConfiguracionAuthDev,
) : AutenticacionRepository {

    private val mutex = Mutex()
    private var token: String? = null
    private var expiraEn: Instant? = null

    override suspend fun tokenVigente(): String = mutex.withLock {
        val actual = token
        val expira = expiraEn
        if (actual != null && expira != null && Instant.now().isBefore(expira)) actual
        else loguearse()
    }

    override suspend fun forzarRelogin(): String = mutex.withLock { loguearse() }

    private suspend fun loguearse(): String {
        val respuesta = api.login(LoginRequestDto(configuracion.dni, configuracion.clave))
        token = respuesta.token
        expiraEn = Instant.parse(respuesta.expiraEn)
        return respuesta.token
    }
}
