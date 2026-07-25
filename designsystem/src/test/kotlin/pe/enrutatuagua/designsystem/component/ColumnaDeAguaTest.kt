package pe.enrutatuagua.designsystem.component

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.height
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua

@RunWith(RobolectricTestRunner::class)
class ColumnaDeAguaTest {
    @get:Rule val regla = createComposeRule()

    @Test
    fun `el agua refleja la fraccion restante`() {
        regla.setContent {
            TemaAgua { ColumnaDeAgua(litrosRestantes = 9000, capacidadLitros = 12000) }
        }
        regla.onNodeWithText("9,000").assertExists()
        regla.onNodeWithText("de 12,000 L").assertExists()

        val tubo = regla.onNodeWithTag("tubo").getUnclippedBoundsInRoot()
        val agua = regla.onNodeWithTag("tubo-agua").getUnclippedBoundsInRoot()
        // 9000/12000 = 75% del tubo (168dp * .75 = 126dp), tolerancia 1dp
        assertEquals(tubo.height.value * 0.75f, agua.height.value, 1f)
    }
}
