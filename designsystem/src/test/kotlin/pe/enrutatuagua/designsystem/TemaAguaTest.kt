package pe.enrutatuagua.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

@RunWith(RobolectricTestRunner::class)
class TemaAguaTest {
    @get:Rule val regla = createComposeRule()

    private var primario: Color = Color.Unspecified
    private var accion: Color = Color.Unspecified

    @Composable
    private fun Sonda() {
        primario = MaterialTheme.colorScheme.primary
        accion = LocalPaletaAgua.current.accion
    }

    @Test
    fun `tema claro expone marca como primary y accion extendida`() {
        regla.setContent { TemaAgua(oscuro = false) { Sonda() } }
        assertEquals(Color(0xFF0D3B66), primario)
        assertEquals(Color(0xFF12A5B0), accion)
    }

    @Test
    fun `tema oscuro cambia la paleta completa`() {
        regla.setContent { TemaAgua(oscuro = true) { Sonda() } }
        assertEquals(Color(0xFF9AD1EA), primario)
        assertEquals(Color(0xFF2ABFC9), accion)
    }
}
