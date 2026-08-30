package pe.enrutatuagua.data.red

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

fun Throwable.mensajeUsuario(fallback: String = "No se pudo completar la operación"): String {
    if (this !is HttpException) return message ?: fallback
    val cuerpo = runCatching { response()?.errorBody()?.string() }.getOrNull() ?: return fallback
    return runCatching { Json.parseToJsonElement(cuerpo).jsonObject["detail"]?.jsonPrimitive?.content }
        .getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
}
