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
    fun `tema claro expone tinta plana como primary y acento operativo`() {
        regla.setContent { TemaAgua(oscuro = false) { Sonda() } }
        assertEquals(Color(0xFF101820), primario)
        assertEquals(Color(0xFF101820), tinta)
        assertEquals(Color(0xFFC62828), error)
        assertEquals(Color(0xFF007C91), accion)
        assertEquals(Color(0xFFE7E9EA), superficie2)
    }

    @Test
    fun `tema oscuro cambia la paleta completa e invierte la tinta`() {
        regla.setContent { TemaAgua(oscuro = true) { Sonda() } }
        assertEquals(Color(0xFFF5F5F5), primario)
        assertEquals(Color(0xFFF5F5F5), tinta)
        assertEquals(Color(0xFFFF6B64), error)
        assertEquals(Color(0xFF31B7C7), accion)
        assertEquals(Color(0xFF2C3032), superficie2)
    }
}
