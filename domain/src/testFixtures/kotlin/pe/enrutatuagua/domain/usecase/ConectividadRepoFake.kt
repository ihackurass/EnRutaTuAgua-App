package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import pe.enrutatuagua.domain.repositorio.ConectividadRepository

class ConectividadRepoFake(inicial: Boolean = false) : ConectividadRepository {
    val conectividadFlow = MutableStateFlow(inicial)

    override fun observarConectividad() = conectividadFlow
}
