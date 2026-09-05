package pe.enrutatuagua.app.ui.presidente

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.app.CustodiaAguaRepoFake
import pe.enrutatuagua.domain.modelo.PaginaRecepciones
import pe.enrutatuagua.domain.modelo.RecepcionPendiente

class RecepcionesViewModelTest {
    private val dispatcher=StandardTestDispatcher()
    private val recepcion=RecepcionPendiente(7,3,"Comité 12",1000,1000,"2026-08-31T10:00:00Z","2026-09-01T10:00:00Z")
    @Before fun antes(){Dispatchers.setMain(dispatcher);CustodiaAguaRepoFake.reiniciar();CustodiaAguaRepoFake.paginaPendientes=PaginaRecepciones(listOf(recepcion),0,20,1,1)}
    @After fun despues(){Dispatchers.resetMain();CustodiaAguaRepoFake.reiniciar()}

    @Test fun `confirmacion exitosa retira la recepcion pendiente`()=runTest {
        val vm=RecepcionesViewModel(CustodiaAguaRepoFake);runCurrent()
        vm.confirmar(recepcion,1000,null);runCurrent()
        assertEquals(1,CustodiaAguaRepoFake.mensajesConfirmacion.size)
        assertFalse(vm.estado.value.pendientes.any{it.rutaPuntoId==7L})
    }

    @Test fun `doble toque inmediato envia una sola peticion`()=runTest {
        val vm=RecepcionesViewModel(CustodiaAguaRepoFake);runCurrent()
        vm.confirmar(recepcion,1000,null)
        vm.confirmar(recepcion,1000,null)
        runCurrent()
        assertEquals(1,CustodiaAguaRepoFake.mensajesConfirmacion.size)
    }

    @Test fun `reintento conserva el mismo mensaje id`()=runTest {
        CustodiaAguaRepoFake.fallosConfirmacion=1
        val vm=RecepcionesViewModel(CustodiaAguaRepoFake);runCurrent()
        vm.confirmar(recepcion,1000,null);runCurrent()
        vm.confirmar(recepcion,1000,null);runCurrent()
        assertEquals(2,CustodiaAguaRepoFake.mensajesConfirmacion.size)
        assertEquals(CustodiaAguaRepoFake.mensajesConfirmacion[0],CustodiaAguaRepoFake.mensajesConfirmacion[1])
    }
}
