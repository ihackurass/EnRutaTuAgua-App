package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
class HojaArrastrableTest {
    @get:Rule val regla = createComposeRule()

    private fun monta(): () -> AnclaHoja? {
        var ancla: AnclaHoja? = null
        regla.setContent {
            TemaAgua {
                HojaArrastrable(onAncla = { ancla = it }) { _ ->
                    Box(Modifier.fillMaxWidth().height(600.dp))
                }
            }
        }
        return { ancla }
    }

    @Test
    fun `arranca en asomo y el arrastre cambia de ancla`() {
        val ancla = monta()
        regla.waitForIdle()
        assertEquals(AnclaHoja.Asomo, ancla())

        regla.onNodeWithTag("asa").performTouchInput {
            swipe(start = center, end = center - Offset(0f, 2000f), durationMillis = 300)
        }
        regla.waitForIdle()
        assertEquals(AnclaHoja.Abierta, ancla())

        regla.onNodeWithTag("asa").performTouchInput {
            swipe(start = center, end = center + Offset(0f, 2000f), durationMillis = 300)
        }
        regla.waitForIdle()
        assertEquals(AnclaHoja.Asomo, ancla())
    }
}
