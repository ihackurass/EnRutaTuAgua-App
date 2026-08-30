package pe.enrutatuagua.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.modelo.Sesion
import pe.enrutatuagua.domain.repositorio.SesionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SesionRepositoryMemoria @Inject constructor() : SesionRepository {
    private val _rol = MutableStateFlow<Rol?>(null)
    private val _sesion = MutableStateFlow<Sesion?>(null)
    override val sesion: Flow<Sesion?> = _sesion
    override val rolActual: Flow<Rol?> = _rol
    override suspend fun iniciar(dni: String, clave: String): Sesion = Sesion(
        1, Rol.entries.toSet(), null, null, "token-prueba", Long.MAX_VALUE,
    ).also { _sesion.value = it }
    override suspend fun seleccionarRol(rol: Rol) {
        _rol.value = rol
        _sesion.value = _sesion.value?.copy(rolActivo = rol)
    }
    override suspend fun cambiarRol() { _rol.value = null; _sesion.value = _sesion.value?.copy(rolActivo = null) }
    override suspend fun cerrar() { _rol.value = null; _sesion.value = null }
    override suspend fun tokenVigente(): String? = _sesion.value?.token
}
