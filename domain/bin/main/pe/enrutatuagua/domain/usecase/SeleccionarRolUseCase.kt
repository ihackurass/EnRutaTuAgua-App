package pe.enrutatuagua.domain.usecase

import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.repositorio.SesionRepository
import javax.inject.Inject

class SeleccionarRolUseCase @Inject constructor(private val sesion: SesionRepository) {
    suspend operator fun invoke(rol: Rol) = sesion.seleccionarRol(rol)
}
