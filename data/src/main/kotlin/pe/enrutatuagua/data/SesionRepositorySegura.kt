package pe.enrutatuagua.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pe.enrutatuagua.data.red.AuthApi
import pe.enrutatuagua.data.red.LoginRequestDto
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.modelo.Sesion
import pe.enrutatuagua.domain.repositorio.SesionRepository

@Singleton
class SesionRepositorySegura @Inject constructor(
    @ApplicationContext contexto: Context,
    private val authApi: AuthApi,
) : SesionRepository {
    private val almacen = AlmacenSesionCifrada(contexto)
    private val _sesion = MutableStateFlow(almacen.leer()?.takeIf { it.expiraEn > System.currentTimeMillis() })
    override val sesion: Flow<Sesion?> = _sesion
    override val rolActual: Flow<Rol?> = _sesion.map { it?.rolActivo }

    init { if (_sesion.value == null) almacen.borrar() }

    override suspend fun iniciar(dni: String, clave: String): Sesion {
        val respuesta = authApi.login(LoginRequestDto(dni.trim(), clave))
        val claims = decodificarClaims(respuesta.token)
        val roles = claims.roles.mapTo(linkedSetOf(), Rol::valueOf)
        require(roles.isNotEmpty()) { "La cuenta no tiene roles habilitados" }
        return Sesion(
            claims.usuarioId, roles, claims.asociacionId, roles.singleOrNull(), respuesta.token,
            minOf(claims.expiraEn, java.time.Instant.parse(respuesta.expiraEn).toEpochMilli()),
        ).also(::guardar)
    }

    override suspend fun seleccionarRol(rol: Rol) {
        val actual = _sesion.value ?: return
        require(rol in actual.roles) { "Rol no autorizado para esta cuenta" }
        guardar(actual.copy(rolActivo = rol))
    }

    override suspend fun cambiarRol() {
        val actual = _sesion.value ?: return
        if (actual.roles.size > 1) guardar(actual.copy(rolActivo = null))
    }

    override suspend fun cerrar() {
        almacen.borrar()
        _sesion.value = null
    }

    override suspend fun tokenVigente(): String? {
        val actual = _sesion.value ?: return null
        if (actual.expiraEn <= System.currentTimeMillis()) {
            cerrar()
            return null
        }
        return actual.token
    }

    private fun guardar(sesion: Sesion) {
        almacen.guardar(sesion)
        _sesion.value = sesion
    }
}

private data class ClaimsSesion(val usuarioId: Long, val roles: List<String>, val asociacionId: Long?, val expiraEn: Long)

private fun decodificarClaims(token: String): ClaimsSesion {
    val payload = token.split('.').getOrNull(1) ?: error("Token inválido")
    val contenido = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
    val json = Json.parseToJsonElement(contenido).jsonObject
    return ClaimsSesion(
        json.getValue("sub").jsonPrimitive.content.toLong(),
        json.getValue("roles").jsonArray.map { it.jsonPrimitive.content },
        json["asoc"]?.jsonPrimitive?.content?.toLong(),
        json.getValue("exp").jsonPrimitive.content.toLong() * 1_000,
    )
}

@Serializable
private data class SesionGuardada(
    val usuarioId: Long, val roles: List<String>, val asociacionId: Long?, val rolActivo: String?,
    val token: String, val expiraEn: Long,
)

private class AlmacenSesionCifrada(contexto: Context) {
    private val preferencias = contexto.getSharedPreferences("sesion_segura", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun guardar(sesion: Sesion) {
        val plano = json.encodeToString(
            SesionGuardada(sesion.usuarioId, sesion.roles.map(Rol::name), sesion.asociacionId,
                sesion.rolActivo?.name, sesion.token, sesion.expiraEn),
        ).toByteArray()
        val cipher = Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, clave()) }
        preferencias.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("datos", Base64.encodeToString(cipher.doFinal(plano), Base64.NO_WRAP))
            .apply()
    }

    fun leer(): Sesion? = runCatching {
        val ivTexto = preferencias.getString("iv", null) ?: return null
        val datosTexto = preferencias.getString("datos", null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMACION).apply {
            init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, Base64.decode(ivTexto, Base64.NO_WRAP)))
        }
        val guardada = json.decodeFromString<SesionGuardada>(
            String(cipher.doFinal(Base64.decode(datosTexto, Base64.NO_WRAP))),
        )
        Sesion(guardada.usuarioId, guardada.roles.mapTo(linkedSetOf(), Rol::valueOf), guardada.asociacionId,
            guardada.rolActivo?.let(Rol::valueOf), guardada.token, guardada.expiraEn)
    }.getOrNull()

    fun borrar() { preferencias.edit().clear().apply() }

    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }

    private companion object {
        const val ALIAS = "enrutatuagua_sesion_v1"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
    }
}
