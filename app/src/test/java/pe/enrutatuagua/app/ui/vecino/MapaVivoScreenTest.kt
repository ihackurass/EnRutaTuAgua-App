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
        // Auditoría rol Vecino: MapaVivoViewModel corre un ticker perpetuo
        // (while(true){emit;delay(30s)}) para refrescar minutosSinSenal --
        // con autoAdvance=true (default) waitForIdle() intenta drenar TODO
        // el trabajo programado, y como el ticker se reprograma a sí mismo
        // para siempre, el test cuelga (confirmado empíricamente: colgó
        // ~10 min hasta matar el daemon). autoAdvance=false congela el
        // reloj de test en vez de intentar agotar un timer infinito.
        regla.mainClock.autoAdvance = false
        regla.setContent {
            TemaAgua {
                MapaVivoScreen(vm = vm, mapa = { _, _ -> Box(Modifier.fillMaxSize()) })
            }
        }
        regla.waitForIdle()

        regla.onNodeWithText("Cisterna en tu zona").assertExists()
        regla.onNodeWithText("Tu asociación").assertExists()
        regla.onNodeWithText("Respaldo REST · hace 47 min").assertExists()
        regla.onNodeWithText("8,500 L").assertExists()
        regla.onNodeWithText("DE 12,000 L").assertExists()   // TarjetaDato uppercasea el rotulo
        regla.onNodeWithText("PUNTOS DE TU ZONA").assertExists()
        regla.onNodeWithText("Comité 12 — Loma Alta").assertExists()
        regla.onNodeWithText("Comité 7 — José C. Mariátegui").assertExists()
        // Auditoría rol Vecino: el ViewModel filtra a sólo VALIDADO -- "AAHH
        // Buena Vista, sector B" (PROPUESTO en TrackingRepoFake) no debe verse.
        regla.onNodeWithText("AAHH Buena Vista, sector B").assertDoesNotExist()
    }

    // Hallazgo real (feedback de producto): sin posición viva, antes se
    // ocultaba el mapa entero detrás de un texto -- ahora el mapa y los
    // puntos de la zona se muestran igual, sólo cambia el badge.
    @Test
    fun `sin posicion muestra el mapa y los puntos igual, con badge de espera`() {
        val repo = TrackingRepoFake(ahoraMs = System.currentTimeMillis())
        repo.posicionFlow.value = null
        val vm = MapaVivoViewModel(repo)
        regla.mainClock.autoAdvance = false
        regla.setContent {
            TemaAgua {
                MapaVivoScreen(vm = vm, mapa = { _, _ -> Box(Modifier.fillMaxSize()) })
            }
        }
        regla.waitForIdle()

        regla.onNodeWithText("Cisterna en tu zona").assertExists()
        regla.onNodeWithText("Esperando la primera posición").assertExists()
        regla.onNodeWithText("PUNTOS DE TU ZONA").assertExists()
        regla.onNodeWithText("Comité 12 — Loma Alta").assertExists()
        regla.onNodeWithText("8,500 L").assertDoesNotExist()
    }
}
