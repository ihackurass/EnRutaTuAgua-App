package pe.enrutatuagua.app.ui.vecino

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.domain.modelo.EstadoValidacionPunto
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.repositorio.TrackingRepository
import pe.enrutatuagua.domain.usecase.TrackingRepoFake

class MapaVivoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun antes() { Dispatchers.setMain(dispatcher) }
    @After fun despues() { Dispatchers.resetMain() }

    @Test fun `expone la ultima posicion y los puntos de la zona ya filtrados a VALIDADO`() = runTest {
        val ahoraMs = 1_000_000_000_000L
        val repo = TrackingRepoFake(ahoraMs = ahoraMs)
        val vm = MapaVivoViewModel(repo)
        vm.ahora = { ahoraMs }
        runCurrent()

        assertEquals(8500, vm.estado.value.posicion?.litrosRestantes)
        // TrackingRepoFake siembra 3 puntos (2 VALIDADO + 1 PROPUESTO) -- el
        // PROPUESTO queda filtrado (auditoría rol Vecino).
        assertEquals(2, vm.estado.value.puntos.size)
        assertEquals(47, vm.estado.value.minutosSinSenal)
    }

    // Hallazgo real (auditoría rol Vecino): sin retry, un error en la carga
    // inicial dejaba el collect muerto para siempre -- mismo patrón ya
    // corregido en RutaViewModel (rol Aguatero).
    @Test fun `un error transitorio en la carga inicial se reintenta y termina mostrando los datos de la zona`() = runTest {
        val ahoraMs = 1_000_000_000_000L
        val repo = TrackingRepoFake(ahoraMs = ahoraMs)
        repo.lanzarEnObservarUltimaPosicion = IllegalStateException("Sin conexión")
        val vm = MapaVivoViewModel(repo)
        vm.ahora = { ahoraMs }

        advanceTimeBy(5_001L)
        runCurrent()

        assertEquals(false, vm.estado.value.cargando)
        assertEquals(8500, vm.estado.value.posicion?.litrosRestantes)
    }

    @Test fun `mientras reintenta la carga inicial expone un mensaje de reintento`() = runTest {
        val repo = TrackingRepoFake()
        repo.lanzarEnObservarUltimaPosicion = IllegalStateException("Sin conexión")
        val vm = MapaVivoViewModel(repo)

        vm.mensaje.test {
            assertEquals(null, awaitItem())
            assertEquals("No pudimos actualizar tu zona. Conservamos los últimos datos.", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Hallazgo real: minutosSinSenal sólo se recalculaba cuando combine()
    // reemitía -- con flows que no vuelven a emitir (como el fake, o un
    // backend real entre ticks de posición), el badge quedaba congelado.
    // El ticker que dispara este recálculo vive en MapaVivoScreen.kt (no en
    // el ViewModel -- un flow perpetuo interno rompía runTest, ver su
    // comentario), así que acá se prueba la función determinista sin
    // avanzar ningún scheduler.
    @Test fun `recalcularMinutosSinSenal actualiza el badge usando el reloj inyectado`() = runTest {
        var ahoraMs = 1_000_000_000_000L
        val repo = TrackingRepoFake(ahoraMs = ahoraMs)
        val vm = MapaVivoViewModel(repo)
        vm.ahora = { ahoraMs }
        runCurrent()
        assertEquals(47, vm.estado.value.minutosSinSenal)

        ahoraMs += 5 * 60_000L // pasan 5 min "reales" sin que ningún flow reemita
        vm.recalcularMinutosSinSenal()

        assertEquals(52, vm.estado.value.minutosSinSenal)
    }

    @Test fun `recalcularMinutosSinSenal sin posicion no hace nada`() = runTest {
        val repo = TrackingRepoFake()
        repo.posicionFlow.value = null
        val vm = MapaVivoViewModel(repo)
        runCurrent()

        vm.recalcularMinutosSinSenal()

        assertEquals(null, vm.estado.value.posicion)
        assertEquals(0, vm.estado.value.minutosSinSenal)
    }

    // Hallazgo real: combine() no producía nada hasta que AMBOS flows
    // emitieran su primer valor -- si uno tardaba más que el otro,
    // cargando=true quedaba pegado para siempre.
    @Test fun `un flujo lento no deja cargando pegado gracias al onStart de cada fuente`() = runTest {
        val posicion = TrackingRepoFake.posicionDePrueba(1_000_000_000_000L)
        val repo = object : TrackingRepository {
            override fun observarUltimaPosicion(): Flow<PosicionCisterna?> = flowOf(posicion)
            override fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>> = flow {
                delay(1_000_000L) // nunca llega a tiempo dentro de este test
            }
        }
        val vm = MapaVivoViewModel(repo)
        runCurrent()

        assertEquals(false, vm.estado.value.cargando)
        assertEquals(emptyList<PuntoAbastecimiento>(), vm.estado.value.puntos)
    }

    @Test fun `los puntos RECHAZADO o SUSPENDIDO no aparecen en la lista filtrada`() = runTest {
        val repo = TrackingRepoFake()
        repo.puntosFlow.value = listOf(
            PuntoAbastecimiento(1, "Validado", -12.0, -76.0, 1000, EstadoValidacionPunto.VALIDADO),
            PuntoAbastecimiento(2, "Rechazado", -12.0, -76.0, 1000, EstadoValidacionPunto.RECHAZADO),
            PuntoAbastecimiento(3, "Suspendido", -12.0, -76.0, 1000, EstadoValidacionPunto.SUSPENDIDO),
        )
        val vm = MapaVivoViewModel(repo)
        runCurrent()

        assertEquals(listOf("Validado"), vm.estado.value.puntos.map { it.nombre })
    }

    @Test fun `al compartir ubicacion identifica la cisterna mas cercana`() = runTest {
        val vm = MapaVivoViewModel(TrackingRepoFake())
        runCurrent()

        vm.actualizarUbicacionVecino(-12.16, -76.94)

        assertEquals(0L, vm.estado.value.rutaMasCercanaId)
    }
}
