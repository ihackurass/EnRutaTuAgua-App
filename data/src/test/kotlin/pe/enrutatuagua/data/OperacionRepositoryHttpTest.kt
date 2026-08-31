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
}
