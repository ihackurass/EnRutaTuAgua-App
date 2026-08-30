package pe.enrutatuagua.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.data.red.RutasApi
import pe.enrutatuagua.data.red.TrackingApi
import retrofit2.HttpException
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentLinkedQueue

private const val RUTA_ZONA_JSON =
    """{"rutaId":29,"vehiculoId":1,"vehiculoCapacidadTanqueLitros":12000,"fecha":"2026-08-02"}"""
private const val POSICION_JSON = """{"rutaId":29,"lat":-12.16,"lng":-76.94,"registradoEn":"2026-08-02T12:00:00Z"}"""
private const val PUNTOS_ZONA_JSON =
    """[{"id":1,"nombre":"Comité 12","lat":-12.16,"lng":-76.94,"capacidadLitros":4000,"estado":"VALIDADO","asociacionId":7}]"""

/** Mismo patrón que RutaRepositoryHttpTest (MockWebServer + un dispatcher por path). */
class TrackingRepositoryHttpTest {
    private lateinit var servidor: MockWebServer
    private lateinit var repo: TrackingRepositoryHttp
    private val colaZona = ConcurrentLinkedQueue<MockResponse>()
    private val colaPosicion = ConcurrentLinkedQueue<MockResponse>()
    private val colaPuntos = ConcurrentLinkedQueue<MockResponse>()

    private fun json(cuerpo: String) = MockResponse().setBody(cuerpo.trim()).addHeader("Content-Type", "application/json")

    @Before fun setUp() {
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/v1/rutas/zona" -> colaZona.poll() ?: MockResponse().setResponseCode(500)
                "/api/v1/tracking/rutas/29/posicion" -> colaPosicion.poll() ?: MockResponse().setResponseCode(500)
                "/api/v1/puntos/zona" -> colaPuntos.poll() ?: MockResponse().setResponseCode(500)
                else -> MockResponse().setResponseCode(404)
            }
        }
        servidor.start()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repo = TrackingRepositoryHttp(
            retrofit.create(RutasApi::class.java),
            retrofit.create(PuntosApi::class.java),
            retrofit.create(TrackingApi::class.java),
        )
    }

    @After fun tearDown() { servidor.shutdown() }

    @Test fun `observarUltimaPosicion resuelve la zona y trae la posicion viva enriquecida con la capacidad del vehiculo`() = runTest {
        colaZona += json(RUTA_ZONA_JSON)
        colaPosicion += json(POSICION_JSON)

        val posicion = repo.observarUltimaPosicion().first()

        assertEquals(-12.16, posicion?.lat)
        assertEquals(12000, posicion?.litrosRestantes)
        assertEquals(12000, posicion?.capacidadLitros)
    }

    @Test fun `observarUltimaPosicion cuando la zona no tiene ruta abierta hoy emite null (404 valido)`() = runTest {
        colaZona += MockResponse().setResponseCode(404).setBody("""{"type":"x"}""")

        val posicion = repo.observarUltimaPosicion().first()

        assertEquals(null, posicion)
    }

    @Test fun `observarUltimaPosicion cuando hay ruta pero sin fix reciente emite null (404 de tracking)`() = runTest {
        colaZona += json(RUTA_ZONA_JSON)
        colaPosicion += MockResponse().setResponseCode(404).setBody("""{"type":"x"}""")

        val posicion = repo.observarUltimaPosicion().first()

        assertEquals(null, posicion)
    }

    @Test fun `observarUltimaPosicion hace polling y refleja una posicion nueva en el siguiente ciclo`() = runTest {
        colaZona += json(RUTA_ZONA_JSON)
        colaPosicion += json(POSICION_JSON)
        colaPosicion += json(POSICION_JSON.replace("-12.16", "-12.20"))

        val posiciones = repo.observarUltimaPosicion().take(2).toList()

        assertEquals(-12.16, posiciones[0]?.lat)
        assertEquals(-12.20, posiciones[1]?.lat)
    }

    @Test fun `un error 500 en la resolucion de zona se propaga como HttpException`() = runTest {
        colaZona += MockResponse().setResponseCode(500)

        try {
            repo.observarUltimaPosicion().first()
            throw AssertionError("debía lanzar")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }
    }

    @Test fun `observarPuntosDeZona trae los puntos de la asociacion del actor`() = runTest {
        colaPuntos += json(PUNTOS_ZONA_JSON)

        val puntos = repo.observarPuntosDeZona().first()

        assertEquals(1, puntos.size)
        assertEquals("Comité 12", puntos[0].nombre)
    }
}
