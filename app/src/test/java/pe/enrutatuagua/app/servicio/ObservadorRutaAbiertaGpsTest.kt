package pe.enrutatuagua.app.servicio

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import pe.enrutatuagua.app.ubicacion.ServicioTrackingGps
import pe.enrutatuagua.domain.modelo.EstadoRuta
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.data.SesionRepositoryMemoria
import pe.enrutatuagua.domain.usecase.RutaRepoFake

/**
 * Dispatchers.Unconfined en vez de StandardTestDispatcher/runTest: acá no
 * hay nada que en verdad suspenda (map/distinctUntilChanged/onEach son
 * síncronos sobre un StateFlow) -- Unconfined resume el colector en el
 * mismo hilo al asignar rutaFlow.value, sin depender de avanzar tiempo
 * virtual.
 *
 * Permiso de ubicación concedido por defecto en @Before: hallazgo real en
 * dispositivo -- arrancar el foreground service (tipo location) sin el
 * permiso tira SecurityException y crashea la app. El observador ahora lo
 * chequea antes de arrancar (ver test dedicado más abajo).
 */
@RunWith(RobolectricTestRunner::class)
class ObservadorRutaAbiertaGpsTest {

    private lateinit var contexto: Application
    private lateinit var repo: RutaRepoFake
    private lateinit var observador: ObservadorRutaAbiertaGps
    private lateinit var sesion: SesionRepositoryMemoria
    private val alcance = CoroutineScope(Dispatchers.Unconfined + Job())

    @Before fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        shadowOf(contexto).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        repo = RutaRepoFake()
        sesion = SesionRepositoryMemoria()
        runBlocking { sesion.seleccionarRol(Rol.AGUATERO) }
        observador = ObservadorRutaAbiertaGps(contexto, repo, sesion)
    }

    @After fun limpiar() { alcance.cancel() }

    @Test fun `arranca el service cuando la ruta pasa a ABIERTA`() {
        repo.rutaFlow.value = null

        observador.iniciar(alcance)
        assertNull(shadowOf(contexto).nextStartedService)

        repo.rutaFlow.value = RutaRepoFake.rutaDePrueba() // nace ABIERTA

        val iniciado = shadowOf(contexto).nextStartedService
        assertNotNull(iniciado)
        assertEquals(ServicioTrackingGps::class.java.name, iniciado?.component?.className)
    }

    @Test fun `detiene el service cuando la ruta se cierra`() {
        repo.rutaFlow.value = RutaRepoFake.rutaDePrueba()

        observador.iniciar(alcance)
        shadowOf(contexto).nextStartedService // consume el arranque inicial

        repo.rutaFlow.value = repo.rutaFlow.value!!.copy(estado = EstadoRuta.CERRADA)

        val detenido = shadowOf(contexto).nextStoppedService
        assertNotNull(detenido)
        assertEquals(ServicioTrackingGps::class.java.name, detenido?.component?.className)
    }

    @Test fun `no reacciona a cambios que no cruzan el estado ABIERTA-CERRADA`() {
        val ruta = RutaRepoFake.rutaDePrueba()
        repo.rutaFlow.value = ruta

        observador.iniciar(alcance)
        shadowOf(contexto).nextStartedService // consume el arranque inicial

        repo.rutaFlow.value = ruta // mismo estado, sin cambio real

        assertNull(shadowOf(contexto).nextStartedService)
    }

    @Test fun `sin permiso de ubicacion no arranca el service de inmediato`() {
        shadowOf(contexto).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        repo.rutaFlow.value = RutaRepoFake.rutaDePrueba() // nace ABIERTA, pero sin permiso

        observador.iniciar(alcance)

        assertNull(shadowOf(contexto).nextStartedService)
    }
}
