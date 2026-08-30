package pe.enrutatuagua.app.ui.rol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.modelo.Sesion
import pe.enrutatuagua.domain.repositorio.SesionRepository

class SesionFake : SesionRepository {
    private val _rol = MutableStateFlow<Rol?>(null)
    override val sesion = MutableStateFlow<Sesion?>(null)
    override val rolActual = _rol
    override suspend fun iniciar(dni: String, clave: String) = Sesion(
        1, setOf(Rol.AGUATERO), null, Rol.AGUATERO, "token", Long.MAX_VALUE,
    ).also { sesion.value = it }
    override suspend fun seleccionarRol(rol: Rol) { _rol.value = rol }
    override suspend fun cambiarRol() { _rol.value = null }
    override suspend fun cerrar() { _rol.value = null; sesion.value = null }
    override suspend fun tokenVigente() = sesion.value?.token
    val valor get() = _rol.value
}

class SesionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun antes() { Dispatchers.setMain(dispatcher) }
    @After fun despues() { Dispatchers.resetMain() }

    @Test fun `seleccionar rol delega al use case`() = runTest {
        val sesion = SesionFake()
        val vm = SesionViewModel(sesion)
        vm.seleccionarRol(Rol.AGUATERO)
        advanceUntilIdle()
        assertEquals(Rol.AGUATERO, sesion.valor)
    }
}
