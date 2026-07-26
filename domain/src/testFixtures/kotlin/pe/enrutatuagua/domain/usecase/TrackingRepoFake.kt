package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento
import pe.enrutatuagua.domain.repositorio.TrackingRepository

class TrackingRepoFake(ahoraMs: Long = System.currentTimeMillis()) : TrackingRepository {
    val posicionFlow = MutableStateFlow<PosicionCisterna?>(posicionDePrueba(ahoraMs))
    val puntosFlow = MutableStateFlow(PedidoRepoFake.puntosDePrueba())

    override fun observarUltimaPosicion(): Flow<PosicionCisterna?> = posicionFlow
    override fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>> = puntosFlow

    companion object {
        fun posicionDePrueba(ahoraMs: Long) = PosicionCisterna(
            lat = -12.1613, lng = -76.9488, litrosRestantes = 8500, capacidadLitros = 12000,
            timestampMs = ahoraMs - 47 * 60_000,  // "hace 47 min" desde que se construye el fake
        )
    }
}
