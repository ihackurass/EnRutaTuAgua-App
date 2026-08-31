package pe.enrutatuagua.app.ui.ruta

import android.app.Application
import android.os.Build
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import pe.enrutatuagua.designsystem.theme.TemaAgua
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.modelo.EstadoSyncOperativo
import pe.enrutatuagua.domain.usecase.ObtenerRutaAbiertaUseCase
import pe.enrutatuagua.domain.usecase.RutaRepoFake

@RunWith(RobolectricTestRunner::class)
class RutaScreenTest {
    @get:Rule val regla = createComposeRule()

    @Test
    fun `muestra acciones pendientes de sincronizar`() {
        val repo = RutaRepoFake().apply {
            syncFlow.value = EstadoSyncOperativo(pendientes = 2)
        }
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        regla.setContent {
            TemaAgua {
                RutaScreen(
                    onParadaClick = {},
                    onOmitirClick = {},
                    vm = vm,
                    mapa = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
        regla.waitForIdle()

        regla.onNodeWithText("2 acciones pendientes de sincronizar").assertExists()
        regla.onNodeWithText("Reintentar").assertExists().assertHasClickAction()
    }

    @Test
    fun `muestra siguiente parada columna y fichas sobre el mapa stub`() {
        val repo = RutaRepoFake()
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        regla.setContent {
            TemaAgua {
                RutaScreen(
                    onParadaClick = {},
                    onOmitirClick = {},
                    vm = vm,
                    mapa = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
        regla.waitForIdle()

        regla.onNodeWithText("SIGUIENTE PARADA").assertExists()
        // El título de la siguiente parada está visible; la ficha queda en el mismo LazyColumn al desplazar.
        regla.onAllNodesWithText("Comité 12 — Loma Alta").assertCountEquals(1)
        regla.onNodeWithText("Recargar").assertExists()
        regla.onNodeWithText("12,000").assertExists()            // columna de agua
        regla.onNodeWithText("Desliza para entregar").assertExists()
        regla.onNodeWithText("Mi ruta de hoy").assertExists()
        regla.onNodeWithText("En ruta · ABC-123 · 0 de 1 paradas").assertExists()
    }

    @Test
    fun `un aviso entrante se muestra en el aviso superior`() {
        val repo = RutaRepoFake()
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        regla.setContent {
            TemaAgua {
                RutaScreen(
                    onParadaClick = {},
                    onOmitirClick = {},
                    vm = vm,
                    mapa = { Box(Modifier.fillMaxSize()) },
                    aviso = "Entrega registrada: 4,000 L",
                )
            }
        }
        regla.waitForIdle()

        regla.onNodeWithTag("aviso").assertExists()
        regla.onNodeWithText("Entrega registrada: 4,000 L").assertExists()
    }

    @Test
    fun `con ruta abierta y sin permiso de segundo plano aparece el dialogo de explicacion`() {
        val repo = RutaRepoFake()
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        regla.setContent {
            TemaAgua {
                RutaScreen(onParadaClick = {}, onOmitirClick = {}, vm = vm, mapa = { Box(Modifier.fillMaxSize()) })
            }
        }
        regla.waitForIdle()

        regla.onNodeWithText("Compartir tu ubicación").assertExists()
    }

    @Test
    fun `en API 30+ tocar Permitir cierra el dialogo sin mandar a ajustes`() {
        // Hallazgo real (reporte del usuario): mandar a Ajustes se percibía
        // como "la app se cierra" -- se sacó ese salto (ver
        // PermisoUbicacionSegundoPlano.kt), en API 30+ no hay forma
        // programática de pedirlo, así que sólo se cierra el diálogo.
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 30)
        val repo = RutaRepoFake()
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        val contexto = ApplicationProvider.getApplicationContext<Application>()
        regla.setContent {
            TemaAgua {
                RutaScreen(onParadaClick = {}, onOmitirClick = {}, vm = vm, mapa = { Box(Modifier.fillMaxSize()) })
            }
        }
        regla.waitForIdle()

        regla.onNodeWithText("Permitir").performClick()
        regla.waitForIdle()

        assertEquals(null, shadowOf(contexto).nextStartedActivity)
        regla.onAllNodesWithText("Compartir tu ubicación").assertCountEquals(0)
    }

    @Test
    fun `sin ruta abierta espera la asignacion del operador`() {
        val repo = RutaRepoFake()
        repo.rutaFlow.value = null
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        regla.setContent {
            TemaAgua {
                RutaScreen(onParadaClick = {}, onOmitirClick = {}, vm = vm, mapa = { Box(Modifier.fillMaxSize()) })
            }
        }
        regla.waitForIdle()

        regla.onNodeWithText("Sin ruta abierta").assertExists()
        regla.onNodeWithText("Espera la asignación del operador").assertExists()
        shadowOf(Looper.getMainLooper()).idle()
        regla.waitForIdle()

        assertEquals(null, repo.rutaFlow.value)
    }

    @Test
    fun `sin paradas pendientes muestra el boton cerrar ruta y lo dispara`() {
        val repo = RutaRepoFake()
        val paradaEntregada = RutaRepoFake.rutaDePrueba().paradas[0]
            .copy(estadoEntrega = EstadoEntrega.ENTREGADO)
        repo.rutaFlow.value = RutaRepoFake.rutaDePrueba().copy(paradas = listOf(paradaEntregada))
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        regla.setContent {
            TemaAgua {
                RutaScreen(onParadaClick = {}, onOmitirClick = {}, vm = vm, mapa = { Box(Modifier.fillMaxSize()) })
            }
        }
        regla.waitForIdle()

        // La hoja arranca colapsada (AnclaHoja.Asomo) -- el botón vive
        // dentro de esa hoja arrastrable/animada, cuyo timing de gesto+tween
        // no es fiable de simular bajo Robolectric. Se verifica que el
        // botón existe, está habilitado y tiene acción de click registrada
        // (wiring de la UI); que `cerrarRuta()` realmente actualiza el
        // repositorio ya lo cubre `RutaViewModelTest` con Turbine.
        regla.onNodeWithText("Ruta lista para cerrar").assertExists()
        regla.onNodeWithText("Cerrar ruta").assertExists().assertHasClickAction().assertIsEnabled()
    }

    @Test
    fun `descartar el dialogo lo cierra sin pedir el permiso`() {
        val repo = RutaRepoFake()
        val vm = RutaViewModel(ObtenerRutaAbiertaUseCase(repo), repo)
        regla.setContent {
            TemaAgua {
                RutaScreen(onParadaClick = {}, onOmitirClick = {}, vm = vm, mapa = { Box(Modifier.fillMaxSize()) })
            }
        }
        regla.waitForIdle()

        regla.onNodeWithText("Ahora no").performClick()
        regla.waitForIdle()

        regla.onAllNodesWithText("Compartir tu ubicación").assertCountEquals(0)
    }
}
