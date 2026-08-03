package pe.enrutatuagua.app.ui.ruta

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.domain.usecase.ObtenerRutaAbiertaUseCase
import pe.enrutatuagua.domain.usecase.RutaRepoFake

class RutaViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun antes() { Dispatchers.setMain(dispatcher) }
    @After fun despues() { Dispatchers.resetMain() }

    @Test fun `expone ruta con tanque calculado y siembra al iniciar`() = runTest {
        val repo = RutaRepoFake()
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        vm.estado.test {
            assertEquals(true, awaitItem().cargando)
            val estado = awaitItem()
            assertEquals(false, estado.cargando)
            assertEquals(12000, estado.rutaConTanque?.litrosRestantes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `abrirRuta llama al repositorio y prende y apaga procesandoAccion`() = runTest {
        val repo = RutaRepoFake()
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        vm.estado.test {
            awaitItem() // cargando=true
            awaitItem() // carga inicial resuelta

            vm.abrirRuta()

            assertEquals(true, awaitItem().procesandoAccion)
            assertEquals(false, awaitItem().procesandoAccion)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `cerrarRuta con error del repo publica un mensaje y no lo deja tildado`() = runTest {
        val repo = RutaRepoFake()
        repo.lanzarEnCerrar = IllegalStateException("Quedan paradas pendientes")
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        vm.estado.test { awaitItem(); awaitItem(); cancelAndIgnoreRemainingEvents() }

        vm.mensaje.test {
            assertEquals(null, awaitItem())
            vm.cerrarRuta()
            assertEquals("Quedan paradas pendientes", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Hallazgo real (auditoría rol Aguatero): sin retry, un error en la
    // carga inicial dejaba el collect muerto para siempre -- había que
    // reabrir la pantalla. Mismo patrón ya usado en ObservadorRutaAbiertaGps.
    @Test fun `un error transitorio en la carga inicial se reintenta y termina mostrando la ruta`() = runTest {
        val repo = RutaRepoFake()
        repo.lanzarEnObservarRutaAbierta = IllegalStateException("Sin conexión")
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)

        vm.estado.test {
            awaitItem() // cargando=true (estado inicial)

            advanceTimeBy(5_001L)
            runCurrent()

            val estado = awaitItem()
            assertEquals(false, estado.cargando)
            assertEquals(12000, estado.rutaConTanque?.litrosRestantes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `mientras reintenta la carga inicial expone un mensaje de reintento`() = runTest {
        val repo = RutaRepoFake()
        repo.lanzarEnObservarRutaAbierta = IllegalStateException("Sin conexión")
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)

        vm.mensaje.test {
            assertEquals(null, awaitItem())
            assertEquals("Sin conexión", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
