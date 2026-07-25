package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import pe.enrutatuagua.domain.modelo.Ruta
import pe.enrutatuagua.domain.modelo.litrosRestantes
import pe.enrutatuagua.domain.repositorio.RutaRepository
import javax.inject.Inject

data class RutaConTanque(val ruta: Ruta, val litrosRestantes: Int)

class ObtenerRutaAbiertaUseCase @Inject constructor(private val repo: RutaRepository) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<RutaConTanque?> =
        repo.observarRutaAbierta().flatMapLatest { ruta ->
            if (ruta == null) flowOf(null)
            else repo.observarEntregas(ruta.id).map { entregas ->
                RutaConTanque(ruta, litrosRestantes(ruta.vehiculo, entregas))
            }
        }
}
