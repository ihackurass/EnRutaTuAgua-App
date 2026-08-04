package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.repositorio.TrackingRepository

class TrackingRepoFake(ahoraMs: Long = System.currentTimeMillis()) : TrackingRepository {
    val posicionFlow = MutableStateFlow<PosicionCisterna?>(posicionDePrueba(ahoraMs))
    val puntosFlow = MutableStateFlow(PedidoRepoFake.puntosDePrueba())

    // Auditoría rol Vecino: mismo patrón auto-reset que RutaRepoFake.lanzarEnObservarRutaAbierta.
    var lanzarEnObservarUltimaPosicion: Exception? = null
    var lanzarEnObservarPuntosDeZona: Exception? = null

    override fun observarUltimaPosicion(): Flow<PosicionCisterna?> = flow {
        lanzarEnObservarUltimaPosicion?.let { lanzarEnObservarUltimaPosicion = null; throw it }
        emitAll(posicionFlow)
    }

    override fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>> = flow {
        lanzarEnObservarPuntosDeZona?.let { lanzarEnObservarPuntosDeZona = null; throw it }
        emitAll(puntosFlow)
    }

    companion object {
        fun posicionDePrueba(ahoraMs: Long) = PosicionCisterna(
            lat = -12.1613, lng = -76.9488, litrosRestantes = 8500, capacidadLitros = 12000,
            timestampMs = ahoraMs - 47 * 60_000,  // "hace 47 min" desde que se construye el fake
        )
    }
}
