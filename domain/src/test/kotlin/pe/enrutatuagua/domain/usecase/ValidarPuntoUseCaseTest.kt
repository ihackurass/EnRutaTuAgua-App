package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.EstadoValidacionPunto

class ValidarPuntoUseCaseTest {
    @Test fun `aprueba un punto PROPUESTO`() = runTest {
        val repo = PedidoRepoFake()  // seed: punto 2 = PROPUESTO
        ValidarPuntoUseCase(repo)(puntoId = 2)
        val punto = repo.puntosFlow.value.first { it.id == 2L }
        assertEquals(EstadoValidacionPunto.VALIDADO, punto.estado)
    }

    @Test fun `un punto ya VALIDADO no se puede volver a validar`() = runTest {
        val repo = PedidoRepoFake()  // seed: punto 1 = VALIDADO
        assertThrows(ErrorDominio.PuntoNoValidable::class.java) {
            kotlinx.coroutines.runBlocking { ValidarPuntoUseCase(repo)(puntoId = 1) }
        }
    }
}
