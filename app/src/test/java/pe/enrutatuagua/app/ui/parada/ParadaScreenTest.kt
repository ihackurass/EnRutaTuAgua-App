package pe.enrutatuagua.app.ui.parada

import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua
import pe.enrutatuagua.domain.usecase.ObtenerRutaAbiertaUseCase
import pe.enrutatuagua.domain.usecase.OmitirParadaUseCase
import pe.enrutatuagua.domain.usecase.RegistrarEntregaUseCase
import pe.enrutatuagua.domain.usecase.RutaRepoFake

@RunWith(RobolectricTestRunner::class)
class ParadaScreenTest {
    @get:Rule val regla = createComposeRule()

    private fun montar(omitir: Boolean = false) {
        val repo = RutaRepoFake()
        val vm = ParadaViewModel(
            RegistrarEntregaUseCase(repo, generarId = { "uuid-1" }, ahora = { 0L }),
            OmitirParadaUseCase(repo),
            ObtenerRutaAbiertaUseCase(repo),
            SavedStateHandle(mapOf("paradaId" to 10L, "omitir" to omitir)),
        )
        regla.setContent { TemaAgua { ParadaScreen(onHecho = {}, vm = vm) } }
        regla.waitForIdle()
    }

    @Test
    fun `precarga el pedido y el deslizador lo anuncia`() {
        montar()
        regla.onNodeWithText("Desliza para confirmar 4,000 L").assertExists()
    }

    @Test
    fun `si excede el tanque la cifra alerta y pide corregir`() {
        montar()
        // 4,000 precargado; dos 9 -> 400,099 L > 12,000 L
        regla.onNodeWithTag("tecla-9").performClick()
        regla.onNodeWithTag("tecla-9").performClick()
        regla.onNodeWithText("Corrige la cantidad").assertExists()
        regla.onNodeWithText(
            "Quedan 12,000 L en el tanque. Baja la cantidad o recarga antes de entregar.",
        ).assertExists()
    }

    @Test
    fun `omitir exige motivo y OTRO exige comentario`() {
        montar(omitir = true)
        regla.onNodeWithText("Registrar omisión").assertIsNotEnabled()
        regla.onNodeWithTag("motivo-3").performClick()   // OTRO
        regla.onNodeWithText("Registrar omisión").assertIsNotEnabled()
        regla.onNodeWithTag("motivo-1").performClick()   // NADIE_PRESENTE
        regla.onNodeWithText("Registrar omisión").assertIsEnabled()
    }
}
