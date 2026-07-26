package pe.enrutatuagua.app.ui.presidente

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
import pe.enrutatuagua.domain.modelo.EstadoPedido
import pe.enrutatuagua.domain.usecase.CancelarPedidoUseCase
import pe.enrutatuagua.domain.usecase.CrearPedidoUseCase
import pe.enrutatuagua.domain.usecase.PedidoRepoFake

class MisPedidosViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun antes() { Dispatchers.setMain(dispatcher) }
    @After fun despues() { Dispatchers.resetMain() }

    @Test fun `el estado inicial refleja los pedidos del fake`() = runTest {
        val repo = PedidoRepoFake()
        CrearPedidoUseCase(repo)(puntoId = 1, litros = 2000)
        val vm = MisPedidosViewModel(repo, CancelarPedidoUseCase(repo))

        vm.estado.test {
            assertEquals(true, awaitItem().cargando)
            val estado = awaitItem()
            assertEquals(false, estado.cargando)
            assertEquals(1, estado.pedidos.size)
            assertEquals(2000, estado.pedidos.single().items.single().litros)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `cancelar un pedido PENDIENTE lo mueve a CANCELADO en el estado expuesto`() = runTest {
        val repo = PedidoRepoFake()
        val creado = CrearPedidoUseCase(repo)(puntoId = 1, litros = 2000)
        val vm = MisPedidosViewModel(repo, CancelarPedidoUseCase(repo))

        vm.estado.test {
            awaitItem() // cargando = true
            awaitItem() // primer valor con el pedido PENDIENTE
            vm.cancelar(creado.id)
            val actualizado = awaitItem()
            assertEquals(EstadoPedido.CANCELADO, actualizado.pedidos.single().estado)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
