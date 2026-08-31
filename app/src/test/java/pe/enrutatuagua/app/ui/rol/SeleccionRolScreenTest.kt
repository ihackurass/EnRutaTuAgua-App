package pe.enrutatuagua.app.ui.rol

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToIndex
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua
import pe.enrutatuagua.domain.modelo.Rol

@RunWith(RobolectricTestRunner::class)
class SeleccionRolScreenTest {
    @get:Rule val regla = createComposeRule()

    @Test
    fun `muestra todos los roles y notifica el elegido`() {
        var elegido: Rol? = null
        regla.setContent { TemaAgua { SeleccionRolScreen(onRolElegido = { elegido = it }) } }

        regla.onNode(hasTestTag("rol-aguatero") and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        regla.runOnIdle { assertEquals(Rol.AGUATERO, elegido) }

        Rol.entries.forEachIndexed { indice, rol ->
            regla.onNode(hasScrollToIndexAction()).performScrollToIndex(indice + 1)
            regla.onNodeWithText(rol.etiqueta).assertExists()
        }
    }

    @Test
    fun `solo muestra los roles disponibles para la sesion`() {
        regla.setContent {
            TemaAgua {
                SeleccionRolScreen(
                    onRolElegido = {},
                    roles = setOf(Rol.PRESIDENTE, Rol.VECINO),
                )
            }
        }

        regla.onNodeWithTag("rol-presidente").assertExists()
        regla.onNodeWithTag("rol-vecino").assertExists()
        regla.onNodeWithTag("rol-aguatero").assertDoesNotExist()
        regla.onNodeWithTag("rol-operador").assertDoesNotExist()
    }
}
