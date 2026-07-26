package pe.enrutatuagua.app.ui.presidente

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.domain.usecase.CrearPedidoUseCase
import pe.enrutatuagua.domain.usecase.PedidoRepoFake

class NuevoPedidoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: PedidoRepoFake
    private lateinit var vm: NuevoPedidoViewModel

    @Before fun antes() {
        Dispatchers.setMain(dispatcher)
        repo = PedidoRepoFake()
        vm = NuevoPedidoViewModel(repo, CrearPedidoUseCase(repo))
    }
    @After fun despues() { Dispatchers.resetMain() }

    @Test fun `expone solo los puntos VALIDADO de la zona (RN-03)`() = runTest {
        advanceUntilIdle()
        assertEquals(2, vm.puntos.value.size)
        assertEquals(true, vm.puntos.value.all { it.estado.name == "VALIDADO" })
    }

    @Test fun `crear con litros validos marca hecho y el pedido aparece en el fake`() = runTest {
        vm.crear(puntoId = 1, litros = 2000); advanceUntilIdle()
        assertEquals(true, vm.hecho.value)
        assertEquals(1, repo.pedidosFlow.value.size)
        assertEquals(2000, repo.pedidosFlow.value.single().items.single().litros)
    }

    @Test fun `litros 0 expone error y no marca hecho`() = runTest {
        vm.crear(puntoId = 1, litros = 0); advanceUntilIdle()
        assertEquals(false, vm.hecho.value)
        assertNotNull(vm.error.value)
    }
}
