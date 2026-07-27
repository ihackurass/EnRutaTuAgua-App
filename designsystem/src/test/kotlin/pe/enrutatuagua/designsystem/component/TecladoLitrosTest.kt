package pe.enrutatuagua.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua

@RunWith(RobolectricTestRunner::class)
class TecladoLitrosTest {
    @get:Rule val regla = createComposeRule()

    private fun monta(inicial: String = ""): () -> String {
        var valor by mutableStateOf(inicial)
        regla.setContent { TemaAgua { TecladoLitros(valor = valor, onCambio = { valor = it }) } }
        return { valor }
    }

    @Test
    fun `digitos concatenan y cero inicial se reemplaza`() {
        val valor = monta("0")
        regla.onNodeWithTag("tecla-3").performClick()
        regla.onNodeWithTag("tecla-5").performClick()
        assertEquals("35", valor())
    }

    @Test
    fun `C limpia y borrar quita el ultimo`() {
        val valor = monta("350")
        regla.onNodeWithTag("tecla-borrar").performClick()
        assertEquals("35", valor())
        regla.onNodeWithTag("tecla-C").performClick()
        assertEquals("", valor())
    }

    @Test
    fun `la tecla de borrar es un icono accesible`() {
        monta("35")
        regla.onNodeWithContentDescription("Borrar dígito").assertExists()
    }

    @Test
    fun `corta en seis digitos`() {
        val valor = monta("999999")
        regla.onNodeWithTag("tecla-1").performClick()
        assertEquals("999999", valor())
    }
}
