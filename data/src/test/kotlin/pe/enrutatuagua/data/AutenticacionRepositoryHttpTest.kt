package pe.enrutatuagua.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.data.red.AuthApi
import pe.enrutatuagua.domain.repositorio.ConfiguracionAuthDev
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit

class AutenticacionRepositoryHttpTest {
    private lateinit var servidor: MockWebServer
    private lateinit var repo: AutenticacionRepositoryHttp

    @Before fun setUp() {
        servidor = MockWebServer()
        servidor.start()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val configuracion = ConfiguracionAuthDev(baseUrl = servidor.url("/").toString(), dni = "10000003", clave = "aguatero1234")
        repo = AutenticacionRepositoryHttp(retrofit.create(AuthApi::class.java), configuracion)
    }

    @After fun tearDown() { servidor.shutdown() }

    private fun respuestaLogin(token: String, expiraEn: String) = MockResponse()
        .setBody("""{"token":"$token","expiraEn":"$expiraEn"}""")
        .addHeader("Content-Type", "application/json")

    @Test fun `tokenVigente hace login la primera vez`() = runTest {
        servidor.enqueue(respuestaLogin("tok-1", "2999-01-01T00:00:00Z"))

        val token = repo.tokenVigente()

        assertEquals("tok-1", token)
        assertEquals(1, servidor.requestCount)
    }

    @Test fun `tokenVigente no repite login si el token todavia no vencio`() = runTest {
        servidor.enqueue(respuestaLogin("tok-1", "2999-01-01T00:00:00Z"))

        repo.tokenVigente()
        val segundo = repo.tokenVigente()

        assertEquals("tok-1", segundo)
        assertEquals(1, servidor.requestCount)
    }

    @Test fun `tokenVigente vuelve a loguearse si el token ya vencio`() = runTest {
        servidor.enqueue(respuestaLogin("tok-1", "2000-01-01T00:00:00Z"))
        servidor.enqueue(respuestaLogin("tok-2", "2999-01-01T00:00:00Z"))

        repo.tokenVigente()
        val segundo = repo.tokenVigente()

        assertEquals("tok-2", segundo)
        assertEquals(2, servidor.requestCount)
    }

    @Test fun `forzarRelogin ignora el token cacheado y pide uno nuevo`() = runTest {
        servidor.enqueue(respuestaLogin("tok-1", "2999-01-01T00:00:00Z"))
        servidor.enqueue(respuestaLogin("tok-2", "2999-01-01T00:00:00Z"))

        repo.tokenVigente()
        val nuevo = repo.forzarRelogin()

        assertEquals("tok-2", nuevo)
        assertEquals(2, servidor.requestCount)
    }
}
