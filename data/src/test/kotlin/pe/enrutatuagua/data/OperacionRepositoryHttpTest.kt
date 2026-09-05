package pe.enrutatuagua.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.data.red.OperacionApi
import pe.enrutatuagua.domain.modelo.EstadoOperacionAguatero
import retrofit2.Retrofit

class OperacionRepositoryHttpTest {
    private lateinit var servidor: MockWebServer
    private lateinit var repo: OperacionRepositoryHttp

    @Before fun preparar() {
        servidor = MockWebServer().also { it.start() }
        val retrofit = Retrofit.Builder().baseUrl(servidor.url("/"))
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
        repo = OperacionRepositoryHttp(retrofit.create(OperacionApi::class.java))
    }

    @After fun cerrar() = servidor.shutdown()

    @Test fun `resumen consulta la fecha y conserva el motivo de indisponibilidad`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody("""
            {"fecha":"2026-09-01","asociaciones":[],"vehiculos":[{"id":4,"placa":"ABC-123","capacidadLitros":5000,"disponible":false,"motivoNoDisponible":"Ya tiene una ruta ese día"}],"aguateros":[]}
        """.trimIndent()))

        val resumen = repo.resumen(LocalDate.parse("2026-09-01"))

        assertEquals("/api/v1/operacion/resumen?fecha=2026-09-01", servidor.takeRequest().path)
        assertFalse(resumen.vehiculos.single().disponible)
        assertEquals("Ya tiene una ruta ese día", resumen.vehiculos.single().motivoNoDisponible)
        assertEquals(null, resumen.vehiculos.single().conductorId)
    }

    @Test fun `auditoria convierte fecha y conserva actor`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody("""
            [{"id":7,"categoria":"AGUA","referencia":"abc","accion":"RECARGA","detalle":"{\"litros\": 500}","ocurridoEn":"2026-09-01T15:30:00Z","actorId":3,"actorNombre":"Aguatero Uno"}]
        """.trimIndent()))

        val evento = repo.auditoria().single()

        assertEquals("/api/v1/operacion/auditoria?limite=100", servidor.takeRequest().path)
        assertEquals("RECARGA", evento.accion)
        assertEquals("Aguatero Uno", evento.actorNombre)
    }

    @Test fun `estado actual conserva asignacion y saldo ligados a la misma ruta`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody("""
            {"estado":"CARGA_CONFIRMADA","ruta":{"ruta":{"id":5,"fecha":"2026-08-31","asociacionId":1,"vehiculoId":2,"aguateroId":3,"litros":1000,"estado":3},"placa":"ABC-123","capacidadLitros":12000},"jornada":{"id":4,"vehiculoId":2,"rutaId":5,"capacidadLitros":12000,"litrosDisponibles":3000}}
        """.trimIndent()))

        val operacion = repo.operacionAguatero()

        assertEquals("/api/v1/operacion/aguatero/actual", servidor.takeRequest().path)
        assertEquals(EstadoOperacionAguatero.CARGA_CONFIRMADA, operacion.estado)
        assertEquals(5L, operacion.asignacion?.ruta?.id)
        assertEquals(5L, operacion.jornada?.rutaId)
        assertEquals(3000, operacion.jornada?.litrosDisponibles)
    }

    @Test fun `carga inicial envia idempotencia y litros a la ruta asignada`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody("""
            {"id":4,"vehiculoId":2,"rutaId":5,"capacidadLitros":12000,"litrosDisponibles":1000}
        """.trimIndent()))

        val saldo = repo.cargarRuta(5, "123e4567-e89b-12d3-a456-426614174000", 1000)
        val peticion = servidor.takeRequest()

        assertEquals("/api/v1/operacion/rutas/5/carga-inicial", peticion.path)
        assertEquals("POST", peticion.method)
        assertEquals("""{"mensajeId":"123e4567-e89b-12d3-a456-426614174000","litros":1000}""", peticion.body.readUtf8())
        assertEquals(5L, saldo.rutaId)
        assertEquals(1000, saldo.litrosDisponibles)
    }
}
