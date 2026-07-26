package pe.enrutatuagua.app.ui.vecino

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua
import pe.enrutatuagua.domain.usecase.TrackingRepoFake

@RunWith(RobolectricTestRunner::class)
class MapaVivoScreenTest {
    @get:Rule val regla = createComposeRule()

    @Test
    fun `muestra cabecera, badge sin conexion, litros y puntos de la zona sobre el mapa stub`() {
        val repo = TrackingRepoFake(ahoraMs = System.currentTimeMillis())
        val vm = MapaVivoViewModel(repo)
        regla.setContent {
            TemaAgua {
                MapaVivoScreen(vm = vm, mapa = { _, _ -> Box(Modifier.fillMaxSize()) })
            }
        }
        regla.waitForIdle()

        regla.onNodeWithText("Tu zona").assertExists()
        regla.onNodeWithText("AA.HH. Villa Solidaridad").assertExists()
        regla.onNodeWithText("Sin conexión en vivo · hace 47 min").assertExists()
        regla.onNodeWithText("8,500 L").assertExists()
        regla.onNodeWithText("DE 12,000 L").assertExists()   // TarjetaDato uppercasea el rotulo
        regla.onNodeWithText("PUNTOS DE TU ZONA").assertExists()
        regla.onNodeWithText("Comité 12 — Loma Alta").assertExists()
        regla.onNodeWithText("AAHH Buena Vista, sector B").assertExists()
        regla.onNodeWithText("Comité 7 — José C. Mariátegui").assertExists()
    }
}
