package pe.enrutatuagua.app.ui.rol

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua

@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {
    @get:Rule val regla = createComposeRule()

    @Test
    fun `muestra un acceso rapido para cada rol de prueba`() {
        regla.setContent {
            TemaAgua {
                LoginScreen(false, null, { _, _ -> }, true)
            }
        }

        regla.onNodeWithTag("acceso-presidente").assertExists()
        regla.onNodeWithTag("acceso-aguatero").assertExists()
        regla.onNodeWithTag("acceso-vecino").assertExists()
    }
}
