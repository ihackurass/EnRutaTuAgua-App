package pe.enrutatuagua.app.ui.ruta

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
}
