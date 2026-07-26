package pe.enrutatuagua.app.ui.vecino

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.domain.usecase.TrackingRepoFake

class MapaVivoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun antes() { Dispatchers.setMain(dispatcher) }
    @After fun despues() { Dispatchers.resetMain() }

    @Test fun `expone la ultima posicion y los puntos de la zona`() = runTest {
        // ahoraMs = reloj real: MapaVivoViewModel calcula minutosSinSenal contra
        // System.currentTimeMillis() (no inyectado, ver nota de diseño en el VM),
        // asi que el fake debe sembrarse desde el mismo reloj para ser determinista.
        val repo = TrackingRepoFake(ahoraMs = System.currentTimeMillis())
        val vm = MapaVivoViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(8500, vm.estado.value.posicion?.litrosRestantes)
        assertEquals(3, vm.estado.value.puntos.size)
        assertEquals(47, vm.estado.value.minutosSinSenal)
    }
}
