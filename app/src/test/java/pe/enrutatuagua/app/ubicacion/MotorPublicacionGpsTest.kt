package pe.enrutatuagua.app.ubicacion

import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.domain.repositorio.ContextoTracking
import pe.enrutatuagua.domain.repositorio.ContextoTrackingRepository
import pe.enrutatuagua.domain.usecase.PosicionGpsRepoFake
import pe.enrutatuagua.domain.usecase.PublicadorPosicionMqttFake

@RunWith(RobolectricTestRunner::class)
class MotorPublicacionGpsTest {

    private lateinit var repo: PosicionGpsRepoFake
    private lateinit var publicador: PublicadorPosicionMqttFake
    private lateinit var motor: MotorPublicacionGps

    @Before fun antes() {
        repo = PosicionGpsRepoFake()
        publicador = PublicadorPosicionMqttFake()
        motor = MotorPublicacionGps(
            repo, publicador,
            object : ContextoTrackingRepository {
                override suspend fun actual() = ContextoTracking(aguateroId = 9, rutaId = 42)
            },
        )
    }

    private fun fix(lat: Double, lng: Double) =
        Location(LocationManager.GPS_PROVIDER).apply { latitude = lat; longitude = lng }

    @Test fun `guarda local aunque el publish falle`() = runTest {
        publicador.proximoResultado = false

        motor.procesarFix(fix(-12.16, -76.94))

        assertEquals(1, repo.guardadas.size)
        assertEquals(-12.16, repo.guardadas[0].lat, 1e-6)
        assertEquals(42L, repo.guardadas[0].rutaId)
    }

    @Test fun `si el publish tiene exito marca la posicion como publicada`() = runTest {
        publicador.proximoResultado = true

        motor.procesarFix(fix(-12.16, -76.94))

        assertTrue(repo.guardadas.isEmpty()) // el fake la borra al marcarPublicadas
        assertEquals(1, publicador.publicadas.size)
        assertEquals(9L, publicador.publicadas[0].first)
    }

    @Test fun `telefono detenido no transmite el ruido gps como movimiento`() = runTest {
        publicador.proximoResultado=true
        motor.procesarFix(fix(-12.160000,-76.940000))
        motor.procesarFix(fix(-12.160030,-76.940025))
        assertEquals(2,publicador.publicadas.size)
        assertEquals(publicador.publicadas[0].second.lat,publicador.publicadas[1].second.lat,0.0)
        assertEquals(publicador.publicadas[0].second.lng,publicador.publicadas[1].second.lng,0.0)
    }

    @Test fun `drenar reintenta las pendientes y las marca publicadas si tienen exito`() = runTest {
        publicador.proximoResultado = false
        motor.procesarFix(fix(-12.16, -76.94))
        assertEquals(1, repo.guardadas.size)

        publicador.proximoResultado = true
        motor.drenar()

        assertTrue(repo.guardadas.isEmpty())
        assertEquals(1, publicador.publicadas.size)
    }

    @Test fun `el ciclo de drenaje periodico corre cada 30s`() = runTest {
        publicador.proximoResultado = false
        motor.procesarFix(fix(-12.16, -76.94))

        motor.iniciarDrenajePeriodico(this)
        publicador.proximoResultado = true
        advanceTimeBy(30_001L)
        runCurrent()

        assertTrue(repo.guardadas.isEmpty())
        assertEquals(1, publicador.publicadas.size)

        motor.detener() // cancela el loop infinito antes de que termine el test
    }

    // Hallazgo real (auditoría rol Aguatero): sin try/catch dentro del
    // while(true), un solo fallo de drenar() mataba el loop entero para
    // siempre -- el buffer de GPS dejaba de drenarse por el resto de la ruta.
    @Test fun `un ciclo de drenaje que falla no mata el loop -- el siguiente ciclo sigue corriendo`() = runTest {
        publicador.proximoResultado = false
        motor.procesarFix(fix(-12.16, -76.94)) // deja 1 pendiente

        repo.lanzarUnaVezEnPendientes = RuntimeException("fallo simulado")
        motor.iniciarDrenajePeriodico(this)

        advanceTimeBy(30_001L); runCurrent() // ciclo 1: drenar() explota, se traga
        assertEquals(1, repo.guardadas.size) // sigue pendiente, no se perdió

        publicador.proximoResultado = true
        advanceTimeBy(30_000L); runCurrent() // ciclo 2: corre normal

        assertTrue(repo.guardadas.isEmpty())
        assertEquals(1, publicador.publicadas.size)

        motor.detener()
    }
}
