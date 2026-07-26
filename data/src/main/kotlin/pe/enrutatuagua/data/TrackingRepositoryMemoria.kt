package pe.enrutatuagua.data

import kotlinx.coroutines.flow.Flow
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.repositorio.TrackingRepository
import pe.enrutatuagua.domain.usecase.TrackingRepoFake
import javax.inject.Inject
import javax.inject.Singleton

// Mismo motivo que PedidoRepositoryMemoria (§0 del plan 06): no existe el
// módulo de Tracking (ADR-2) que alimentaría un WebSocket real, así que no
// hay nada que Room deba persistir. Se reutiliza el fake como
// implementación real-de-mentira en producción.
@Singleton
class TrackingRepositoryMemoria @Inject constructor() : TrackingRepository {
    private val fake = TrackingRepoFake()

    override fun observarUltimaPosicion(): Flow<PosicionCisterna?> = fake.observarUltimaPosicion()
    override fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>> = fake.observarPuntosDeZona()
}
