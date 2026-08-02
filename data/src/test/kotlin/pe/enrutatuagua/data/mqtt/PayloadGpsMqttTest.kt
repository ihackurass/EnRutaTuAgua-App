package pe.enrutatuagua.data.mqtt

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.enrutatuagua.domain.repositorio.PosicionGps
import java.time.Instant

class PayloadGpsMqttTest {

    @Test fun `topico usa el aguateroId como segmento del path`() {
        assertEquals("gps/9/posicion", PayloadGpsMqtt.topico(9))
    }

    @Test fun `json tiene el shape exacto que espera el backend`() {
        val momento = Instant.parse("2026-07-27T10:00:00Z")
        val posicion = PosicionGps(rutaId = 42, lat = -12.16, lng = -76.94, registradaEnMillis = momento.toEpochMilli())

        val json = PayloadGpsMqtt.json(posicion)

        assertEquals(
            "{\"rutaId\":42,\"lat\":-12.16,\"lng\":-76.94,\"registradoEn\":\"2026-07-27T10:00:00Z\"}",
            json,
        )
    }
}
