package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.MotivoOmision

class OmitirParadaUseCaseTest {
    private val repo = RutaRepoFake()
    private val useCase = OmitirParadaUseCase(repo)

    @Test
    fun `omite con motivo de catalogo`() = runTest {
        useCase(paradaId = 10, motivo = MotivoOmision.CALLE_CERRADA, comentario = null)
        assertEquals(MotivoOmision.CALLE_CERRADA, repo.omitida?.second)
    }

    @Test
    fun `motivo OTRO exige comentario`() = runTest {
        assertThrows(ErrorDominio.ComentarioRequerido::class.java) {
            runBlocking { useCase(10, MotivoOmision.OTRO, comentario = "  ") }
        }
    }
}
