package pe.enrutatuagua.app.ui.presidente

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pe.enrutatuagua.designsystem.theme.TemaAgua
import pe.enrutatuagua.domain.usecase.CancelarPedidoUseCase
import pe.enrutatuagua.domain.usecase.CrearPedidoUseCase
import pe.enrutatuagua.domain.usecase.PedidoRepoFake
import pe.enrutatuagua.domain.usecase.ValidarPuntoUseCase

/**
 * `PresidenteNav()` (producción) resuelve sus ViewModels con `hiltViewModel()`
 * sin parámetros propios — no es invocable en un test Robolectric puro sin un
 * arnés de Hilt para tests, que no existe en este proyecto (fuera de alcance
 * de esta ejecución: solo se toca `ui/presidente/`, y montar Hilt para tests
 * requeriría tocar `build.gradle.kts`/`AndroidManifest` fuera de esa carpeta).
 * Igual que el resto de la suite (`ParadaScreenTest`, `RutaScreenTest`, que
 * también evitan `hiltViewModel()` inyectando el `vm` a mano), este test
 * reconstruye el mismo grafo de rutas — exactamente las mismas 3 rutas y el
 * mismo cableado de callbacks que `PresidenteNav.kt` — con ViewModels
 * explícitos respaldados por `PedidoRepoFake`, y verifica el comportamiento
 * de navegación real punta a punta.
 *
 * `qualifiers` fija un tamaño de pantalla realista: el default de Robolectric
 * (sin device config) es una ventana diminuta (~320x470dp) donde el propio
 * `NuevoPedidoScreen` (selector de puntos + cifra + teclado + botón, igual de
 * denso que `ParadaScreen`) no entra completo — el teclado quedaría recortado
 * y `performClick()` no llega a las teclas fuera del viewport medido.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp")
class PresidenteNavTest {
    @get:Rule val regla = createComposeRule()

    @Composable
    private fun GrafoDePrueba(
        misPedidosVm: MisPedidosViewModel,
        nuevoPedidoVm: NuevoPedidoViewModel,
        validarPuntosVm: ValidarPuntosViewModel,
    ) {
        val nav = rememberNavController()
        NavHost(nav, startDestination = "mis-pedidos") {
            composable("mis-pedidos") {
                MisPedidosScreen(
                    onNuevoPedido = { nav.navigate("nuevo-pedido") },
                    onValidarPuntos = { nav.navigate("validar-puntos") },
                    vm = misPedidosVm,
                )
            }
            composable("nuevo-pedido") { NuevoPedidoScreen(onHecho = { nav.popBackStack() }, vm = nuevoPedidoVm) }
            composable("validar-puntos") { ValidarPuntosScreen(onVolver = { nav.popBackStack() }, vm = validarPuntosVm) }
        }
    }

    private fun montar(repo: PedidoRepoFake) {
        regla.setContent {
            TemaAgua {
                GrafoDePrueba(
                    MisPedidosViewModel(repo, CancelarPedidoUseCase(repo)),
                    NuevoPedidoViewModel(repo, CrearPedidoUseCase(repo)),
                    ValidarPuntosViewModel(repo, ValidarPuntoUseCase(repo)),
                )
            }
        }
        regla.waitForIdle()
    }

    @Test
    fun `tocar Nuevo pedido navega a NuevoPedidoScreen`() {
        montar(PedidoRepoFake())

        regla.onNodeWithText("MIS PEDIDOS").assertExists()
        regla.onNodeWithText("Nuevo pedido").performClick()
        regla.waitForIdle()

        regla.onNodeWithText("NUEVO PEDIDO").assertExists()
    }

    @Test
    fun `crear un pedido vuelve a MisPedidosScreen y aparece en la lista`() {
        montar(PedidoRepoFake())

        regla.onNodeWithText("Nuevo pedido").performClick()
        regla.waitForIdle()

        // Comité 12 — Loma Alta es VALIDADO en el seed del fake (RN-03)
        regla.onNodeWithText("Comité 12 — Loma Alta").performClick()
        regla.waitForIdle()
        regla.onNodeWithText("Elegido").assertExists()

        regla.onNodeWithTag("tecla-2").performClick()
        regla.onNodeWithTag("tecla-0").performClick()
        regla.onNodeWithTag("tecla-0").performClick()
        regla.onNodeWithTag("tecla-0").performClick()
        regla.waitForIdle()
        regla.onNodeWithText("2,000").assertExists()

        regla.onNodeWithText("Pedir agua").assertIsEnabled()
        regla.onNodeWithText("Pedir agua").performClick()
        regla.waitForIdle()

        regla.onNodeWithText("MIS PEDIDOS").assertExists()
        regla.onNodeWithText("Pedido #1").assertExists()
        regla.onNodeWithText("2,000 L").assertExists()
    }
}
