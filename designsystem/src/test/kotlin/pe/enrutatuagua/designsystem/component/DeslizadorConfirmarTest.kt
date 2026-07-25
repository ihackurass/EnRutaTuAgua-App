package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.designsystem.theme.TemaAgua

@RunWith(RobolectricTestRunner::class)
class DeslizadorConfirmarTest {
    @get:Rule val regla = createComposeRule()

    private fun monta(): () -> Int {
        var confirmaciones = 0
        regla.setContent {
            TemaAgua {
                Box(Modifier.width(300.dp)) {
                    DeslizadorConfirmar(texto = "Desliza para entregar",
                        onConfirmar = { confirmaciones++ })
                }
            }
        }
        return { confirmaciones }
    }

    @Test
    fun `arrastre corto no confirma`() {
        val n = monta()
        regla.onNodeWithTag("perilla").performTouchInput {
            swipe(start = center, end = center + Offset(60f, 0f), durationMillis = 500)
        }
        regla.waitForIdle()
        assertEquals(0, n())
    }

    @Test
    fun `arrastre completo confirma exactamente una vez`() {
        val n = monta()
        regla.onNodeWithTag("perilla").performTouchInput {
            swipe(start = center, end = center + Offset(3000f, 0f), durationMillis = 300)
        }
        regla.waitForIdle()
        assertEquals(1, n())
    }
}
