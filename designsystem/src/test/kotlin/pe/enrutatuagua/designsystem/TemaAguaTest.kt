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
    private var error: Color = Color.Unspecified
    private var accion: Color = Color.Unspecified
    private var tinta: Color = Color.Unspecified
    private var superficie2: Color = Color.Unspecified

    @Composable
    private fun Sonda() {
        primario = MaterialTheme.colorScheme.primary
        error = MaterialTheme.colorScheme.error
        accion = LocalPaletaAgua.current.accion
        tinta = LocalPaletaAgua.current.tinta
        superficie2 = LocalPaletaAgua.current.superficie2
    }

    @Test
    fun `tema claro expone tinta como primary y accion extendida`() {
        regla.setContent { TemaAgua(oscuro = false) { Sonda() } }
        assertEquals(Color(0xFF0E1B2A), primario)
        assertEquals(Color(0xFF0E1B2A), tinta)
        assertEquals(Color(0xFFE11900), error)
        assertEquals(Color(0xFF0A7F90), accion)
        assertEquals(Color(0xFFF0F2F4), superficie2)
    }

    @Test
    fun `tema oscuro cambia la paleta completa e invierte la tinta`() {
        regla.setContent { TemaAgua(oscuro = true) { Sonda() } }
        assertEquals(Color(0xFFE9F2F7), primario)
        assertEquals(Color(0xFFE9F2F7), tinta)
        assertEquals(Color(0xFFFF5C48), error)
        assertEquals(Color(0xFF2CC7D3), accion)
        assertEquals(Color(0xFF1D2933), superficie2)
    }
}
