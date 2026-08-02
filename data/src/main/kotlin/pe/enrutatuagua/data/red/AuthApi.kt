package pe.enrutatuagua.data.red

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class LoginRequestDto(val dni: String, val clave: String)

@Serializable
data class LoginResponseDto(val token: String, val expiraEn: String)

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body solicitud: LoginRequestDto): LoginResponseDto
}
