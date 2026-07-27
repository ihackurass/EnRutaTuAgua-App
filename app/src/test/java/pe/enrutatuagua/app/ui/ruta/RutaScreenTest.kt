package pe.enrutatuagua.app.ui.ruta

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua
import pe.enrutatuagua.domain.usecase.ObtenerRutaAbiertaUseCase
import pe.enrutatuagua.domain.usecase.RutaRepoFake

@RunWith(RobolectricTestRunner::class)
class RutaScreenTest {
    @get:Rule val regla = createComposeRule()

    @Test
    fun `muestra siguiente parada columna y fichas sobre el mapa stub`() {
        val repo = RutaRepoFake()
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        regla.setContent {
            TemaAgua {
                RutaScreen(
                    onParadaClick = {},
                    onOmitirClick = {},
                    vm = vm,
                    mapa = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
        regla.waitForIdle()

        regla.onNodeWithText("SIGUIENTE PARADA").assertExists()
        // titulo grande + ficha en la lista = 2 apariciones
        regla.onAllNodesWithText("Comité 12 — Loma Alta").assertCountEquals(2)
        regla.onNodeWithText("12,000").assertExists()            // columna de agua
        regla.onNodeWithText("Desliza para entregar").assertExists()
        regla.onNodeWithText("Mi ruta de hoy").assertExists()
        regla.onNodeWithText("Cisterna ABC-123 · 0 de 1 paradas").assertExists()
        regla.onNodeWithText("EN RUTA").assertExists()            // chip de estado
    }

    @Test
    fun `un aviso entrante se muestra en el aviso superior`() {
        val repo = RutaRepoFake()
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        regla.setContent {
            TemaAgua {
                RutaScreen(
                    onParadaClick = {},
                    onOmitirClick = {},
                    vm = vm,
                    mapa = { Box(Modifier.fillMaxSize()) },
                    aviso = "Entrega registrada: 4,000 L",
                )
            }
        }
        regla.waitForIdle()

        regla.onNodeWithTag("aviso").assertExists()
        regla.onNodeWithText("Entrega registrada: 4,000 L").assertExists()
    }
}
