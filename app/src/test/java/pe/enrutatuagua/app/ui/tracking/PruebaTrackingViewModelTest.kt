package pe.enrutatuagua.app.ui.tracking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.app.ubicacion.AnclaTracking
import pe.enrutatuagua.app.ubicacion.ProveedorAnclaTracking
import pe.enrutatuagua.app.ubicacion.ResultadoAnclaTracking
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.repositorio.ContextoTracking
import pe.enrutatuagua.domain.repositorio.ContextoTrackingRepository
import pe.enrutatuagua.domain.repositorio.PosicionGps
import pe.enrutatuagua.domain.repositorio.PublicadorPosicionMqtt
import pe.enrutatuagua.domain.repositorio.TrackingRepository

@OptIn(ExperimentalCoroutinesApi::class)
class PruebaTrackingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun antes() = Dispatchers.setMain(dispatcher)
    @After fun despues() = Dispatchers.resetMain()

    @Test fun `el recorrido se construye en metros alrededor de cualquier ancla`() {
        val ancla = AnclaTracking(-12.0464, -77.0428, 1L)
        val punto = desplazarMetros(ancla, norteMetros = 32.0, esteMetros = 28.0)
        val distanciaNorte = (punto.lat - ancla.lat) * 111_320.0
        val distanciaEste = (punto.lng - ancla.lng) * 111_320.0 *
            kotlin.math.cos(Math.toRadians(ancla.lat))
        assertEquals(32.0, distanciaNorte, 0.01)
        assertEquals(28.0, distanciaEste, 0.01)
    }

    @Test fun `sin ubicacion activa termina y explica como reintentar`() = runTest(dispatcher) {
        val vm = crearVm(ResultadoAnclaTracking.UbicacionDesactivada)
        vm.alternar()
        runCurrent()
        assertFalse(vm.estado.value.activa)
        assertTrue(vm.estado.value.mensaje.contains("Activa la ubicación"))
    }

    @Test fun `publica el primer punto en el ancla real y no en Villa Maria`() = runTest(dispatcher) {
        val ancla = AnclaTracking(-12.0464, -77.0428, System.currentTimeMillis())
        val publicados = mutableListOf<PosicionGps>()
        val vm = crearVm(ResultadoAnclaTracking.Disponible(ancla), publicados)
        vm.alternar()
        runCurrent()
        assertEquals(1, publicados.size)
        assertEquals(ancla.lat, publicados.single().lat, 0.000001)
        assertEquals(ancla.lng, publicados.single().lng, 0.000001)
        vm.detener()
    }

    private fun crearVm(
        resultado: ResultadoAnclaTracking,
        publicados: MutableList<PosicionGps> = mutableListOf(),
    ): PruebaTrackingViewModel {
        val proveedor = object : ProveedorAnclaTracking {
            override suspend fun obtener() = resultado
        }
        val contexto = object : ContextoTrackingRepository {
            override suspend fun actual() = ContextoTracking(3, 29)
        }
        val publicador = object : PublicadorPosicionMqtt {
            override suspend fun publicar(aguateroId: Long, posicion: PosicionGps): Boolean {
                publicados += posicion
                return true
            }
        }
        val tracking = object : TrackingRepository {
            private val posicion = MutableStateFlow<PosicionCisterna?>(null)
            override fun observarUltimaPosicion() = posicion
            override fun observarPuntosDeZona() = MutableStateFlow(emptyList<pe.enrutatuagua.domain.modelo.PuntoAbastecimiento>())
        }
        return PruebaTrackingViewModel(contexto, publicador, tracking, proveedor)
    }
}
