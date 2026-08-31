package pe.enrutatuagua.app.ui.rol

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
        regla.onNodeWithTag("acceso-operador").assertExists()
    }

    @Test
    fun `permite mostrar y volver a ocultar la clave`() {
        regla.setContent {
            TemaAgua {
                LoginScreen(false, null, { _, _ -> })
            }
        }

        regla.onNodeWithContentDescription("Mostrar contraseña").performClick()
        regla.onNodeWithContentDescription("Ocultar contraseña").assertExists().performClick()
        regla.onNodeWithContentDescription("Mostrar contraseña").assertExists()
    }
}
