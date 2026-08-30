package pe.enrutatuagua.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.data.red.RutasApi
import pe.enrutatuagua.data.red.VehiculosApi
import pe.enrutatuagua.domain.modelo.Entrega
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.modelo.EstadoRuta
import pe.enrutatuagua.domain.modelo.MotivoOmision
import pe.enrutatuagua.domain.modelo.TipoEntrega
import retrofit2.HttpException
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentLinkedQueue

private const val RUTA_JSON = """
{"id":29,"aguateroId":3,"fecha":"2026-08-02","estado":"ABIERTA","vehiculoId":1,
 "paradas":[{"id":10,"puntoId":1,"orden":1,"capacidadEsperadaLitros":500,"estadoEntrega":"PENDIENTE"}],
 "cerradaEn":null}
"""
private const val VEHICULO_JSON = """{"id":1,"placa":"ABC-123","capacidadTanqueLitros":12000}"""
private const val PUNTO_JSON = """{"id":1,"nombre":"Comité 12","lat":-12.16,"lng":-76.94,"capacidadLitros":4000}"""

/**
 * `vehiculosApi.porId`/`puntosApi.porId` se piden en PARALELO
 * (`RutaRepositoryHttp.enriquecer`) -- un `enqueue()` FIFO simple no sirve
 * porque el orden de llegada real al server no es determinista. Se
 * despachan por path (siempre la misma respuesta, no se consumen); sólo
 * las llamadas a `/rutas/...` (inherentemente secuenciales, una por
 * acción del test) usan una cola.
 */
class RutaRepositoryHttpTest {
    private lateinit var servidor: MockWebServer
    private lateinit var repo: RutaRepositoryHttp
    private val colaRutas = ConcurrentLinkedQueue<MockResponse>()

    private fun json(cuerpo: String) = MockResponse().setBody(cuerpo.trim()).addHeader("Content-Type", "application/json")

    @Before fun setUp() {
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/v1/vehiculos/1" -> json(VEHICULO_JSON)
                "/api/v1/puntos/1" -> json(PUNTO_JSON)
                else -> colaRutas.poll() ?: MockResponse().setResponseCode(500)
            }
        }
        servidor.start()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repo = RutaRepositoryHttp(
            retrofit.create(RutasApi::class.java),
            retrofit.create(PuntosApi::class.java),
            retrofit.create(VehiculosApi::class.java),
        )
    }

    @After fun tearDown() { servidor.shutdown() }

    @Test fun `observarRutaAbierta enriquece vehiculo y punto`() = runTest {
        colaRutas += json(RUTA_JSON)

        val ruta = repo.observarRutaAbierta().first()

        assertEquals(29L, ruta?.id)
        assertEquals("ABC-123", ruta?.vehiculo?.placa)
        assertEquals("Comité 12", ruta?.paradas?.get(0)?.punto?.nombre)
    }

    @Test fun `observarRutaAbierta con 404 emite null en vez de explotar`() = runTest {
        colaRutas += MockResponse().setResponseCode(404).setBody("""{"type":"x"}""")

        val ruta = repo.observarRutaAbierta().first()

        assertEquals(null, ruta)
    }

    @Test fun `abrirRuta usa directo la respuesta del POST, sin GET extra`() = runTest {
        colaRutas += json(RUTA_JSON)

        val ruta = repo.abrirRuta()

        assertEquals(29L, ruta.id)
        val vista = servidor.takeRequest()
        assertEquals("POST", vista.method)
        assertTrue(vista.path!!.endsWith("/rutas/abrir"))
    }

    @Test fun `cerrarRuta pega al id de la ruta cargada y deja observarRutaAbierta en null`() = runTest {
        colaRutas += json(RUTA_JSON)
        repo.observarRutaAbierta().first()
        colaRutas += json(RUTA_JSON.replace("\"ABIERTA\"", "\"CERRADA\""))

        val cerrada = repo.cerrarRuta()

        assertEquals(EstadoRuta.CERRADA, cerrada.estado)
        // carga inicial: GET abierta + GET vehiculo + GET punto (los 2 últimos en paralelo, sin orden fijo entre sí)
        repeat(3) { servidor.takeRequest() }
        val vistaCerrar = servidor.takeRequest()
        assertTrue(vistaCerrar.path!!.endsWith("/rutas/29/cerrar"))
        // Hallazgo real (T8): "abierta" sólo representa la ruta ABIERTA -- si
        // se guardara la CERRADA ahí, la pantalla nunca volvería al estado
        // vacío (nada filtra por estado en la UI).
        assertEquals(null, repo.observarRutaAbierta().first())
    }

    @Test fun `registrarEntrega publica y parchea la parada localmente, sin GET extra`() = runTest {
        colaRutas += json(RUTA_JSON)
        repo.observarRutaAbierta().first()
        val entregaEcoJson = """{"id":"e1","rutaPuntoId":10,"tipo":"ENTREGA","litros":500,"registradaEn":"2026-01-01T00:00:00Z"}"""
        colaRutas += MockResponse().setResponseCode(201).setBody(entregaEcoJson)

        repo.registrarEntrega(Entrega("e1", 10, TipoEntrega.ENTREGA, null, 500, null, null, 0L))

        // Hallazgo real (T8): un GET /rutas/abierta justo después del POST
        // podía traer el estado ANTERIOR de la parada (carrera reproducible
        // contra el backend real) -- se parchea con lo que el propio POST ya
        // confirmó, sin una vuelta de red extra.
        val ruta = repo.observarRutaAbierta().first()
        assertEquals(EstadoEntrega.ENTREGADO, ruta?.paradas?.get(0)?.estadoEntrega)
        // carga inicial (ruta+vehiculo+punto) + POST entregas = 4, sin refresco extra
        assertEquals(4, servidor.requestCount)
    }

    @Test fun `registrar una cantidad menor conserva la parada como parcial y muestra el saldo`() = runTest {
        colaRutas += json(RUTA_JSON)
        repo.observarRutaAbierta().first()
        colaRutas += MockResponse().setResponseCode(201).setBody(
            """{"id":"e-parcial","rutaPuntoId":10,"tipo":"ENTREGA","litros":300,"registradaEn":"2026-01-01T00:00:00Z"}""",
        )

        repo.registrarEntrega(Entrega("e-parcial", 10, TipoEntrega.ENTREGA, null, 300, null, null, 0L))

        val parada = repo.observarRutaAbierta().first()!!.paradas.single()
        assertEquals(EstadoEntrega.PARCIAL, parada.estadoEntrega)
        assertEquals(300, parada.litrosEntregados)
        assertEquals(200, parada.capacidadEsperadaLitros - parada.litrosEntregados)
    }

    @Test fun `omitirParada publica y parchea motivo y comentario localmente`() = runTest {
        colaRutas += json(RUTA_JSON)
        repo.observarRutaAbierta().first()
        colaRutas += json("""{"id":10,"puntoId":1,"orden":1,"capacidadEsperadaLitros":500,"estadoEntrega":"OMITIDO","motivoOmision":"NADIE_PRESENTE"}""")

        repo.omitirParada(10L, MotivoOmision.NADIE_PRESENTE, null)

        val ruta = repo.observarRutaAbierta().first()
        assertEquals(EstadoEntrega.OMITIDO, ruta?.paradas?.get(0)?.estadoEntrega)
        assertEquals(MotivoOmision.NADIE_PRESENTE, ruta?.paradas?.get(0)?.motivoOmision)
        assertEquals(4, servidor.requestCount)
    }

    @Test fun `omitirParada sin ruta cargada lanza IllegalStateException`() = runTest {
        try {
            repo.omitirParada(10L, MotivoOmision.CALLE_CERRADA, null)
            throw AssertionError("debía lanzar")
        } catch (e: IllegalStateException) {
            // esperado
        }
    }

    @Test fun `un error 500 en abierta se propaga como HttpException`() = runTest {
        colaRutas += MockResponse().setResponseCode(500)

        try {
            repo.observarRutaAbierta().first()
            throw AssertionError("debía lanzar")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }
    }
}
