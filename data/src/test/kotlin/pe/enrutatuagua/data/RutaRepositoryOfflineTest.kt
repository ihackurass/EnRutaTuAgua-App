package pe.enrutatuagua.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.data.local.EnRutaTuAguaDb
import pe.enrutatuagua.data.red.PuntosApi
import pe.enrutatuagua.data.red.RutasApi
import pe.enrutatuagua.data.red.VehiculosApi
import pe.enrutatuagua.domain.modelo.Entrega
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.modelo.TipoEntrega
import pe.enrutatuagua.domain.repositorio.ConectividadRepository
import retrofit2.Retrofit

private const val RUTA_OFFLINE_JSON = """
{"id":29,"aguateroId":3,"fecha":"2026-08-02","estado":"ABIERTA","vehiculoId":1,
 "paradas":[{"id":10,"puntoId":1,"orden":1,"capacidadEsperadaLitros":500,"estadoEntrega":"PENDIENTE"}],
 "cerradaEn":null}
"""
private const val VEHICULO_OFFLINE_JSON = """{"id":1,"placa":"ABC-123","capacidadTanqueLitros":12000}"""
private const val PUNTO_OFFLINE_JSON = """{"id":1,"nombre":"Comité 12","lat":-12.16,"lng":-76.94,"capacidadLitros":4000}"""

@RunWith(RobolectricTestRunner::class)
class RutaRepositoryOfflineTest {
    private lateinit var servidor: MockWebServer
    private lateinit var db: EnRutaTuAguaDb
    private lateinit var repo: RutaRepositoryHttp
    private val sinRed = AtomicBoolean(false)
    private val errorServidor = AtomicBoolean(false)
    private val conectividad = MutableStateFlow(false)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EnRutaTuAguaDb::class.java,
        ).allowMainThreadQueries().build()
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/v1/rutas/abierta" -> json(RUTA_OFFLINE_JSON)
                "/api/v1/vehiculos/1" -> json(VEHICULO_OFFLINE_JSON)
                "/api/v1/puntos/1" -> json(PUNTO_OFFLINE_JSON)
                "/api/v1/sync" -> when {
                    sinRed.get() -> MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
                    errorServidor.get() -> MockResponse().setResponseCode(503)
                    else -> json(
                        """{"entregas":[{"id":"entrega-offline","ok":true,"resultado":"CREADA"}],"omisiones":[]}""",
                    )
                }
                else -> MockResponse().setResponseCode(404)
            }
        }
        servidor.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(OkHttpClient.Builder().callTimeout(300, TimeUnit.MILLISECONDS).build())
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()),
            )
            .build()
        repo = RutaRepositoryHttp(
            retrofit.create(RutasApi::class.java),
            retrofit.create(PuntosApi::class.java),
            retrofit.create(VehiculosApi::class.java),
            db,
            object : ConectividadRepository {
                override fun observarConectividad() = conectividad
            },
        )
    }

    @After
    fun tearDown() {
        db.close()
        servidor.shutdown()
    }

    @Test
    fun `una entrega sobrevive sin red y se elimina solo tras confirmacion`() = runTest {
        repo.observarRutaAbierta().first()
        sinRed.set(true)

        repo.registrarEntrega(
            Entrega("entrega-offline", 10, TipoEntrega.ENTREGA, null, 300, null, null, 0L),
        )

        val pendiente = db.accionOperativaDao().porId("entrega-offline")
        assertEquals("PENDIENTE", pendiente?.estado)
        assertEquals(1, pendiente?.intentos)
        assertEquals(1, repo.observarEstadoSync().first().pendientes)
        assertEquals(EstadoEntrega.PARCIAL, repo.observarRutaAbierta().first()!!.paradas.single().estadoEntrega)

        sinRed.set(false)
        repo.reintentarSync()

        assertEquals(null, db.accionOperativaDao().porId("entrega-offline"))
        assertEquals(0, repo.observarEstadoSync().first().pendientes)
        val cuerposSync = (0 until servidor.requestCount)
            .map { servidor.takeRequest().body.readUtf8() }
            .filter { it.contains("entrega-offline") }
        assertTrue(cuerposSync.size >= 2)
        assertTrue(cuerposSync.all { it.contains("\"id\":\"entrega-offline\"") })
    }

    @Test
    fun `un error temporal del servidor conserva la entrega como pendiente`() = runTest {
        repo.observarRutaAbierta().first()
        errorServidor.set(true)

        repo.registrarEntrega(
            Entrega("entrega-503", 10, TipoEntrega.ENTREGA, null, 100, null, null, 0L),
        )

        val pendiente = db.accionOperativaDao().porId("entrega-503")
        assertEquals("PENDIENTE", pendiente?.estado)
        assertEquals(1, pendiente?.intentos)
        assertEquals("Servidor no disponible (503)", pendiente?.error)
    }

    private fun json(cuerpo: String) = MockResponse()
        .setBody(cuerpo.trim())
        .addHeader("Content-Type", "application/json")
}
