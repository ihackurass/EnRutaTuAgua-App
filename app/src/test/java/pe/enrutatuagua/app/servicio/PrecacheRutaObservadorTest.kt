package pe.enrutatuagua.app.servicio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.modelo.EstadoRuta
import pe.enrutatuagua.domain.usecase.ConectividadRepoFake
import pe.enrutatuagua.domain.usecase.DescargadorRegionMapaFake
import pe.enrutatuagua.domain.usecase.RutaRepoFake
import pe.enrutatuagua.data.SesionRepositoryMemoria
import pe.enrutatuagua.domain.modelo.Rol
import kotlinx.coroutines.runBlocking

/** Dispatchers.Unconfined: mismo motivo que ObservadorRutaAbiertaGpsTest -- todo síncrono sobre StateFlow. */
@RunWith(RobolectricTestRunner::class)
class PrecacheRutaObservadorTest {

    private lateinit var rutaRepo: RutaRepoFake
    private lateinit var conectividadRepo: ConectividadRepoFake
    private lateinit var descargador: DescargadorRegionMapaFake
    private lateinit var observador: PrecacheRutaObservador
    private val alcance = CoroutineScope(Dispatchers.Unconfined + Job())

    @Before fun preparar() {
        rutaRepo = RutaRepoFake()
        conectividadRepo = ConectividadRepoFake()
        descargador = DescargadorRegionMapaFake()
        val sesion = SesionRepositoryMemoria()
        runBlocking { sesion.iniciar("", ""); sesion.seleccionarRol(Rol.AGUATERO) }
        observador = PrecacheRutaObservador(rutaRepo, conectividadRepo, descargador, sesion)
    }

    @After fun limpiar() { alcance.cancel() }

    @Test fun `no dispara sin conexion aunque haya ruta abierta`() {
        rutaRepo.rutaFlow.value = RutaRepoFake.rutaDePrueba()
        conectividadRepo.conectividadFlow.value = false

        observador.iniciar(alcance)

        assertTrue(descargador.descargas.isEmpty())
    }

    @Test fun `no dispara sin ruta abierta aunque haya conexion`() {
        rutaRepo.rutaFlow.value = null
        conectividadRepo.conectividadFlow.value = true

        observador.iniciar(alcance)

        assertTrue(descargador.descargas.isEmpty())
    }

    @Test fun `dispara cuando hay ruta abierta y conexion`() {
        rutaRepo.rutaFlow.value = RutaRepoFake.rutaDePrueba()
        conectividadRepo.conectividadFlow.value = false

        observador.iniciar(alcance)
        assertTrue(descargador.descargas.isEmpty())

        conectividadRepo.conectividadFlow.value = true

        assertEquals(1, descargador.descargas.size)
    }

    @Test fun `no re-dispara si solo cambia el estado de entrega de una parada`() {
        val ruta = RutaRepoFake.rutaDePrueba()
        rutaRepo.rutaFlow.value = ruta
        conectividadRepo.conectividadFlow.value = true

        observador.iniciar(alcance)
        assertEquals(1, descargador.descargas.size)

        rutaRepo.rutaFlow.value = ruta.copy(
            paradas = listOf(ruta.paradas[0].copy(estadoEntrega = EstadoEntrega.ENTREGADO)),
        )

        assertEquals(1, descargador.descargas.size)
    }

    @Test fun `re-dispara si cambia la geometria de las paradas`() {
        val ruta = RutaRepoFake.rutaDePrueba()
        rutaRepo.rutaFlow.value = ruta
        conectividadRepo.conectividadFlow.value = true

        observador.iniciar(alcance)
        assertEquals(1, descargador.descargas.size)

        val parada = ruta.paradas[0]
        rutaRepo.rutaFlow.value = ruta.copy(
            paradas = listOf(parada.copy(punto = parada.punto.copy(lat = parada.punto.lat + 0.5))),
        )

        assertEquals(2, descargador.descargas.size)
    }

    @Test fun `deja de disparar cuando la ruta se cierra`() {
        val ruta = RutaRepoFake.rutaDePrueba()
        rutaRepo.rutaFlow.value = ruta
        conectividadRepo.conectividadFlow.value = true

        observador.iniciar(alcance)
        assertEquals(1, descargador.descargas.size)

        rutaRepo.rutaFlow.value = ruta.copy(estado = EstadoRuta.CERRADA)
        conectividadRepo.conectividadFlow.value = false
        conectividadRepo.conectividadFlow.value = true

        assertEquals(1, descargador.descargas.size)
    }
}
