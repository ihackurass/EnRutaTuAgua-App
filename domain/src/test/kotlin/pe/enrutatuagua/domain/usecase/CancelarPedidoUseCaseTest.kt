package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.EstadoPedido

class CancelarPedidoUseCaseTest {
    @Test fun `cancela un pedido PENDIENTE`() = runTest {
        val repo = PedidoRepoFake()
        val creado = CrearPedidoUseCase(repo)(puntoId = 1, litros = 1000)
        CancelarPedidoUseCase(repo)(creado.id)
        assertEquals(EstadoPedido.CANCELADO, repo.pedidosFlow.value.single().estado)
    }

    @Test fun `un pedido ATENDIDO no se puede cancelar (I-10)`() = runTest {
        val repo = PedidoRepoFake()
        val creado = CrearPedidoUseCase(repo)(puntoId = 1, litros = 1000)
        repo.forzarEstado(creado.id, EstadoPedido.ATENDIDO)  // helper de test, ver fake
        assertThrows(ErrorDominio.PedidoNoCancelable::class.java) {
            kotlinx.coroutines.runBlocking { CancelarPedidoUseCase(repo)(creado.id) }
        }
    }
}
