package pe.enrutatuagua.data.red

import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.modelo.Sesion
import pe.enrutatuagua.domain.repositorio.SesionRepository

private class SesionFake : SesionRepository {
    private val valor = Sesion(3, setOf(Rol.AGUATERO), null, Rol.AGUATERO, "tok-vigente", Long.MAX_VALUE)
    override val sesion = MutableStateFlow<Sesion?>(valor)
    override val rolActual = MutableStateFlow<Rol?>(Rol.AGUATERO)
    var cierres = 0
    override suspend fun iniciar(dni: String, clave: String) = valor
    override suspend fun seleccionarRol(rol: Rol) { rolActual.value = rol }
    override suspend fun cambiarRol() = Unit
    override suspend fun cerrar() { cierres++; sesion.value = null; rolActual.value = null }
    override suspend fun tokenVigente() = sesion.value?.token
}

class InterceptorAutenticacionTest {
    private lateinit var servidor: MockWebServer
    private lateinit var sesion: SesionFake
    private lateinit var cliente: OkHttpClient

    @Before fun setUp() {
        servidor = MockWebServer().also { it.start() }
        sesion = SesionFake()
        cliente = OkHttpClient.Builder().addInterceptor(InterceptorAutenticacion(sesion)).build()
    }

    @After fun tearDown() = servidor.shutdown()

    private fun ejecutar() = cliente.newCall(Request.Builder().url(servidor.url("/algo")).build()).execute()

    @Test fun `adjunta el Authorization Bearer de la sesion`() {
        servidor.enqueue(MockResponse().setResponseCode(200))
        ejecutar().close()
        assertEquals("Bearer tok-vigente", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test fun `ante 401 limpia sesion y no hace relogin automatico`() {
        servidor.enqueue(MockResponse().setResponseCode(401))
        val respuesta = ejecutar()
        assertEquals(401, respuesta.code)
        assertEquals(1, servidor.requestCount)
        assertEquals(1, sesion.cierres)
    }
}
