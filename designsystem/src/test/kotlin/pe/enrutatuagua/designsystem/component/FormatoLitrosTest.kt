package pe.enrutatuagua.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoLitrosTest {
    @Test
    fun `separa miles con coma es-PE`() {
        assertEquals("3,500", formatearLitros(3500))
        assertEquals("12,000", formatearLitros(12000))
        assertEquals("0", formatearLitros(0))
    }
}
