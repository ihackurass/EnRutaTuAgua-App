package pe.enrutatuagua.app.ui.rol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import retrofit2.HttpException
import java.net.SocketTimeoutException
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.modelo.Sesion
import pe.enrutatuagua.domain.repositorio.SesionRepository
import javax.inject.Inject

@HiltViewModel
class SesionViewModel @Inject constructor(
    private val repo: SesionRepository,
) : ViewModel() {
    val sesion: StateFlow<Sesion?> = repo.sesion.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _procesando = MutableStateFlow(false)
    val procesando: StateFlow<Boolean> = _procesando
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun iniciar(dni: String, clave: String) = viewModelScope.launch {
        if (dni.isBlank() || clave.isBlank()) { _error.value = "Ingresa DNI y contraseña"; return@launch }
        _procesando.value = true
        _error.value = null
        try { repo.iniciar(dni, clave) }
        catch (e: HttpException) {
            _error.value = if (e.code() == 401) "DNI o contraseña incorrectos" else "No se pudo iniciar sesión (${e.code()})"
        } catch (_: SocketTimeoutException) {
            _error.value = "El servidor no responde. Verifica la conexión"
        } catch (e: Exception) {
            _error.value = e.message ?: "No se pudo iniciar sesión"
        } finally { _procesando.value = false }
    }

    fun seleccionarRol(rol: Rol) { viewModelScope.launch { repo.seleccionarRol(rol) } }
    fun cambiarRol() { viewModelScope.launch { repo.cambiarRol() } }
    fun cerrar() { viewModelScope.launch { repo.cerrar() } }
}
