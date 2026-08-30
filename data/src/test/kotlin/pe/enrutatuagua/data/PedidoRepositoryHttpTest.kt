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
}
