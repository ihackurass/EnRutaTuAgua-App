package pe.enrutatuagua.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.enrutatuagua.domain.modelo.Entrega
import pe.enrutatuagua.domain.modelo.TipoEntrega

class ObtenerRutaAbiertaUseCaseTest {
    private val repo = RutaRepoFake()
    private val useCase = ObtenerRutaAbiertaUseCase(repo)

    @Test
    fun `combina ruta con tanque calculado, nunca almacenado`() = runTest {
        useCase().test {
            assertEquals(12000, awaitItem()?.litrosRestantes)   // tanque lleno
            repo.registrarEntrega(Entrega("u1", 10, TipoEntrega.ENTREGA, null, 3500, null, null, 0L))
            assertEquals(8500, awaitItem()?.litrosRestantes)    // derivado de entregas (ER §5)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
