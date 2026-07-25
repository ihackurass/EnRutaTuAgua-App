package pe.enrutatuagua.app.ui.parada

import androidx.lifecycle.SavedStateHandle
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
import pe.enrutatuagua.domain.modelo.MotivoOmision
import pe.enrutatuagua.domain.usecase.OmitirParadaUseCase
import pe.enrutatuagua.domain.usecase.RegistrarEntregaUseCase
import pe.enrutatuagua.domain.usecase.RutaRepoFake

class ParadaViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: RutaRepoFake
    private lateinit var vm: ParadaViewModel

    @Before fun antes() {
        Dispatchers.setMain(dispatcher)
        repo = RutaRepoFake()
        vm = ParadaViewModel(
            RegistrarEntregaUseCase(repo, generarId = { "uuid-1" }, ahora = { 0L }),
            OmitirParadaUseCase(repo),
            SavedStateHandle(mapOf("paradaId" to 10L)),
        )
    }
    @After fun despues() { Dispatchers.resetMain() }

    @Test fun `entrega valida marca hecho`() = runTest {
        vm.confirmarEntrega(3500); advanceUntilIdle()
        assertEquals(true, vm.hecho.value)
        assertEquals("uuid-1", repo.registradas.single().id)
    }

    @Test fun `RN-01 violada expone error y no marca hecho`() = runTest {
        vm.confirmarEntrega(99999); advanceUntilIdle()
        assertEquals(false, vm.hecho.value)
        assertNotNull(vm.error.value)
    }

    @Test fun `omitir con motivo marca hecho`() = runTest {
        vm.omitir(MotivoOmision.CALLE_CERRADA, null); advanceUntilIdle()
        assertEquals(true, vm.hecho.value)
        assertEquals(MotivoOmision.CALLE_CERRADA, repo.omitida?.second)
    }
}
