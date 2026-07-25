package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.TipoEntrega

class RegistrarEntregaUseCaseTest {
    private val repo = RutaRepoFake()
    private val useCase = RegistrarEntregaUseCase(repo, generarId = { "uuid-fijo-1" }, ahora = { 0L })

    @Test
    fun `registra entrega valida con UUID generado una sola vez`() = runTest {
        useCase(paradaId = 10, litros = 3500)
        assertEquals(1, repo.registradas.size)
        assertEquals("uuid-fijo-1", repo.registradas[0].id)
        assertEquals(TipoEntrega.ENTREGA, repo.registradas[0].tipo)
    }

    @Test
    fun `RN-01 - rechaza litros mayores al tanque restante`() = runTest {
        useCase(paradaId = 10, litros = 3500)               // quedan 8500
        assertThrows(ErrorDominio.TanqueInsuficiente::class.java) {
            runBlocking { useCase(paradaId = 10, litros = 9000) }
        }
    }

    @Test
    fun `rechaza litros cero o negativos`() = runTest {
        assertThrows(ErrorDominio.LitrosInvalidos::class.java) {
            runBlocking { useCase(paradaId = 10, litros = 0) }
        }
    }
}
