package pe.enrutatuagua.app.ui.rol

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun `muestra los 4 roles y notifica el elegido`() {
        var elegido: Rol? = null
        regla.setContent { TemaAgua { SeleccionRolScreen(onRolElegido = { elegido = it }) } }

        Rol.entries.forEach { regla.onNodeWithText(it.etiqueta).assertExists() }
        regla.onNodeWithText("Aguatero (cisterna)").performClick()
        assertEquals(Rol.AGUATERO, elegido)
    }
}
