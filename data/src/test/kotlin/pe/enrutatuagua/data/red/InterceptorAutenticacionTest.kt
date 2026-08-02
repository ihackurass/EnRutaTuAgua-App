package pe.enrutatuagua.data.red

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.domain.repositorio.AutenticacionRepository

private class AutenticacionRepositoryFake : AutenticacionRepository {
    var llamadasTokenVigente = 0
    var llamadasForzarRelogin = 0

    override suspend fun tokenVigente(): String {
        llamadasTokenVigente++
        return "tok-vigente"
    }

    override suspend fun forzarRelogin(): String {
        llamadasForzarRelogin++
        return "tok-nuevo"
    }
}

class InterceptorAutenticacionTest {
    private lateinit var servidor: MockWebServer
    private lateinit var autenticacion: AutenticacionRepositoryFake
    private lateinit var cliente: OkHttpClient

    @Before fun setUp() {
        servidor = MockWebServer()
        servidor.start()
        autenticacion = AutenticacionRepositoryFake()
        cliente = OkHttpClient.Builder().addInterceptor(InterceptorAutenticacion(autenticacion)).build()
    }

    @After fun tearDown() { servidor.shutdown() }

    private fun ejecutar(): okhttp3.Response {
        val request = Request.Builder().url(servidor.url("/algo")).build()
        return cliente.newCall(request).execute()
    }

    @Test fun `adjunta el Authorization Bearer con el token vigente`() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(200))

        ejecutar()

        val recibida = servidor.takeRequest()
        assertEquals("Bearer tok-vigente", recibida.getHeader("Authorization"))
    }

    @Test fun `ante 401 reintenta una vez con un token nuevo via forzarRelogin`() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(401))
        servidor.enqueue(MockResponse().setResponseCode(200))

        val respuesta = ejecutar()

        assertEquals(200, respuesta.code)
        servidor.takeRequest() // la original, con tok-vigente
        val reintento = servidor.takeRequest()
        assertEquals("Bearer tok-nuevo", reintento.getHeader("Authorization"))
        assertEquals(1, autenticacion.llamadasForzarRelogin)
    }

    @Test fun `si el reintento tambien da 401 no reintenta de nuevo`() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(401))
        servidor.enqueue(MockResponse().setResponseCode(401))

        val respuesta = ejecutar()

        assertEquals(401, respuesta.code)
        assertEquals(2, servidor.requestCount)
        assertEquals(1, autenticacion.llamadasForzarRelogin)
    }
}
