package pe.enrutatuagua.app.ui.mapa

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua

@RunWith(RobolectricTestRunner::class)
class BotonSeguirCamionTest {

    @get:Rule
    val regla = createComposeRule()

    @Test
    fun `sin seguir ofrece Seguir camion y el click llega`() {
        var clicks = 0
        regla.setContent {
            TemaAgua { BotonSeguirCamion(siguiendo = false, onClick = { clicks++ }) }
        }

        regla.onNodeWithContentDescription("Seguir camión").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `siguiendo ofrece Ver ruta completa`() {
        regla.setContent {
            TemaAgua { BotonSeguirCamion(siguiendo = true, onClick = {}) }
        }

        regla.onNodeWithContentDescription("Ver ruta completa").assertExists()
    }
}
