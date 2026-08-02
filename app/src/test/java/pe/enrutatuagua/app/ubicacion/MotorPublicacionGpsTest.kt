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
import pe.enrutatuagua.domain.repositorio.ConfiguracionTrackingDev
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
            ConfiguracionTrackingDev(aguateroId = 9, rutaId = 42, brokerUrl = "tcp://dev:1883"),
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
}
