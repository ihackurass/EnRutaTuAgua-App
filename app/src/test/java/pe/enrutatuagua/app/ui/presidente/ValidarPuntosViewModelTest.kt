package pe.enrutatuagua.app.ui.presidente

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.domain.modelo.EstadoValidacionPunto
import pe.enrutatuagua.domain.usecase.PedidoRepoFake
import pe.enrutatuagua.domain.usecase.ValidarPuntoUseCase

class ValidarPuntosViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: PedidoRepoFake
    private lateinit var vm: ValidarPuntosViewModel

    @Before fun antes() {
        Dispatchers.setMain(dispatcher)
        repo = PedidoRepoFake()
        vm = ValidarPuntosViewModel(repo, ValidarPuntoUseCase(repo))
    }
    @After fun despues() { Dispatchers.resetMain() }

    @Test fun `expone solo los puntos PROPUESTO, filtra los VALIDADO`() = runTest {
        advanceUntilIdle()
        assertEquals(1, vm.puntos.value.size)
        assertEquals(EstadoValidacionPunto.PROPUESTO, vm.puntos.value.single().estado)
    }

    @Test fun `validar un punto lo saca de la lista expuesta`() = runTest {
        advanceUntilIdle()
        val puntoId = vm.puntos.value.single().id
        vm.validar(puntoId); advanceUntilIdle()
        assertEquals(true, vm.puntos.value.isEmpty())
    }
}
