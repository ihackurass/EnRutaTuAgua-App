package pe.enrutatuagua.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.data.red.PedidosApi
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.domain.modelo.PedidoItem
import pe.enrutatuagua.domain.modelo.CondicionesPedido
import pe.enrutatuagua.domain.modelo.PrioridadPedido
import retrofit2.Retrofit

class PedidoRepositoryHttpTest {
    private lateinit var servidor: MockWebServer
    private lateinit var repo: PedidoRepositoryHttp

    @Before fun setUp() {
        servidor = MockWebServer().also { it.start() }
        val retrofit = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
        repo = PedidoRepositoryHttp(
            retrofit.create(PedidosApi::class.java),
            retrofit.create(PuntosApi::class.java),
        )
    }

    @After fun tearDown() = servidor.shutdown()

    @Test fun `crear multipunto envia todos los items en una sola solicitud`() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(201)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"id":8,"items":[{"puntoId":1,"litros":2000},{"puntoId":3,"litros":1500}],"estado":"PENDIENTE","creadoEn":"2026-08-30T10:00:00Z"}"""),
        )

        val creado = repo.crearPedidoMultipunto(listOf(PedidoItem(1, 2_000), PedidoItem(3, 1_500)))

        val solicitud = servidor.takeRequest()
        assertEquals("POST", solicitud.method)
        assertEquals("/api/v1/pedidos", solicitud.path)
        val cuerpo = solicitud.body.readUtf8()
        assertTrue(cuerpo.contains("\"items\":["))
        assertTrue(cuerpo.contains("{\"puntoId\":1,\"litros\":2000}"))
        assertTrue(cuerpo.contains("{\"puntoId\":3,\"litros\":1500}"))
        assertEquals(3_500, creado.items.sumOf { it.litros })
    }

    @Test fun `crear urgente envia motivo y ventana en UTC`() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(201)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"id":9,"items":[{"puntoId":1,"litros":500}],"estado":"PENDIENTE","creadoEn":"2026-08-30T10:00:00Z","prioridad":"URGENTE","motivoPrioridad":"Tanque comunal agotado","ventanaDesde":"2026-08-30T11:00:00Z","ventanaHasta":"2026-08-30T15:00:00Z"}"""),
        )

        repo.crearPedidoMultipunto(
            listOf(PedidoItem(1, 500)),
            CondicionesPedido(
                PrioridadPedido.URGENTE,
                "Tanque comunal agotado",
                1_777_546_800_000,
                1_777_561_200_000,
            ),
        )

        val cuerpo = servidor.takeRequest().body.readUtf8()
        assertTrue(cuerpo.contains("\"prioridad\":\"URGENTE\""))
        assertTrue(cuerpo.contains("\"motivoPrioridad\":\"Tanque comunal agotado\""))
        assertTrue(cuerpo.contains("\"ventanaDesde\":\"2026-04-30T11:00:00Z\""))
        assertTrue(cuerpo.contains("\"ventanaHasta\":\"2026-04-30T15:00:00Z\""))
    }
}
