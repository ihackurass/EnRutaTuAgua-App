package pe.enrutatuagua.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.repositorio.SesionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SesionRepositoryMemoria @Inject constructor() : SesionRepository {
    private val _rol = MutableStateFlow<Rol?>(null)
    override val rolActual: Flow<Rol?> = _rol
    override suspend fun seleccionarRol(rol: Rol) { _rol.value = rol }
}
