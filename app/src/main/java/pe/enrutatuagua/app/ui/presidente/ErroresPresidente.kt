package pe.enrutatuagua.app.ui.presidente

import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pe.enrutatuagua.domain.modelo.ErrorDominio
import retrofit2.HttpException

internal fun Throwable.mensajeParaUsuario(): String = when (this) {
    is ErrorDominio -> message ?: "Revisa los datos ingresados"
    is IOException -> "Sin conexión. Conéctate a internet para continuar"
    is HttpException -> runCatching {
        val cuerpo = response()?.errorBody()?.string().orEmpty()
        val problema = Json.parseToJsonElement(cuerpo).jsonObject
        problema["detail"]?.jsonPrimitive?.content
            ?: problema["title"]?.jsonPrimitive?.content
    }.getOrNull() ?: "No se pudo completar la operación (${code()})"
    else -> "Ocurrió un error. Inténtalo nuevamente"
}
