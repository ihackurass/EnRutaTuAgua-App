package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.enrutatuagua.domain.modelo.ErrorDominio

class CrearPedidoUseCaseTest {
    @Test fun `crea pedido con un item PENDIENTE`() = runTest {
        val repo = PedidoRepoFake()
        val useCase = CrearPedidoUseCase(repo)
        val pedido = useCase(puntoId = 1, litros = 2000)
        assertEquals(1, repo.pedidosFlow.value.size)
        assertEquals(2000, pedido.items.single().litros)
    }

    @Test fun `litros invalidos lanza ErrorDominio`() = runTest {
        val useCase = CrearPedidoUseCase(PedidoRepoFake())
        assertThrows(ErrorDominio.PedidoInvalido::class.java) {
            kotlinx.coroutines.runBlocking { useCase(puntoId = 1, litros = 0) }
        }
    }
}
