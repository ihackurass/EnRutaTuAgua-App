package pe.enrutatuagua.data.mqtt

import pe.enrutatuagua.domain.repositorio.PosicionGps
import java.time.Instant

/**
 * Shape fijo por PuenteGpsMqttKafka (backend, ADR-2) -- 4 campos, sin
 * envoltorio. Se arma el JSON a mano en vez de con org.json.JSONObject: esa
 * clase es un stub sin implementación real fuera de un runtime Android
 * (Robolectric o un dispositivo real), y este shape es demasiado simple
 * para justificar arrastrar Robolectric a un test que no necesita nada más
 * de Android.
 */
object PayloadGpsMqtt {

    fun topico(aguateroId: Long): String = "gps/$aguateroId/posicion"

    fun json(posicion: PosicionGps): String {
        val registradoEn = Instant.ofEpochMilli(posicion.registradaEnMillis)
        return "{\"mensajeId\":\"${posicion.mensajeId}\",\"rutaId\":${posicion.rutaId},\"lat\":${posicion.lat},\"lng\":${posicion.lng}," +
            "\"registradoEn\":\"$registradoEn\"}"
    }
}
