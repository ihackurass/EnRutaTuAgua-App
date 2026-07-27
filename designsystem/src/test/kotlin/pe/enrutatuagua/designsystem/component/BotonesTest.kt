package pe.enrutatuagua.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import compose.icons.TablerIcons
import compose.icons.tablericons.Ban
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua

@RunWith(RobolectricTestRunner::class)
class BotonesTest {
    @get:Rule val regla = createComposeRule()

    @Test
    fun `boton secundario con icono muestra el texto y responde al click`() {
        var clicks = 0
        regla.setContent {
            TemaAgua(oscuro = false) {
                BotonSecundario(
                    texto = "No pude entregar",
                    onClick = { clicks++ },
                    icono = TablerIcons.Ban,
                )
            }
        }
        regla.onNodeWithText("No pude entregar").assertIsDisplayed().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `boton primario deshabilitado no dispara onClick`() {
        var clicks = 0
        regla.setContent {
            TemaAgua(oscuro = false) {
                BotonPrimario(
                    texto = "Registrar omisión",
                    onClick = { clicks++ },
                    habilitado = false,
                )
            }
        }
        regla.onNodeWithText("Registrar omisión").performClick()
        assertEquals(0, clicks)
    }
}
