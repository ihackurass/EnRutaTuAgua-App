package pe.enrutatuagua.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import pe.enrutatuagua.designsystem.theme.TemaAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GaleriaTokensTest {
    @get:Rule val regla = createComposeRule()

    @Composable
    private fun Galeria() {
        val p = LocalPaletaAgua.current
        Column(Modifier.background(p.fondo).fillMaxWidth().padding(16.dp)) {
            Row {
                listOf(p.marca, p.accion, p.alerta, p.exito, p.hoja, p.tenue).forEach { c ->
                    Box(Modifier.size(40.dp).padding(2.dp).background(c))
                }
            }
            Text("12,000", style = EstilosAgua.CifraHero, color = p.texto)
            Text("Mz. H Lt. 18", style = EstilosAgua.TituloParada, color = p.texto)
            Text("PARADAS DE HOY", style = EstilosAgua.Rotulo, color = p.texto2)
            Text("Fam. Ramos Cauna · pidió 2,000 L", style = EstilosAgua.Cuerpo, color = p.texto2)
        }
    }

    @Test
    fun `galeria clara`() {
        regla.setContent { TemaAgua(oscuro = false) { Galeria() } }
        regla.onRoot().captureRoboImage("src/test/screenshots/galeria_clara.png")
    }

    @Test
    fun `galeria oscura`() {
        regla.setContent { TemaAgua(oscuro = true) { Galeria() } }
        regla.onRoot().captureRoboImage("src/test/screenshots/galeria_oscura.png")
    }
}
