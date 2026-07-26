package pe.enrutatuagua.domain.repositorio

import kotlinx.coroutines.flow.Flow
import pe.enrutatuagua.domain.modelo.*

interface RutaRepository {
    fun observarRutaAbierta(): Flow<Ruta?>
    fun observarEntregas(rutaId: Long): Flow<List<Entrega>>
    suspend fun registrarEntrega(entrega: Entrega)
    suspend fun omitirParada(paradaId: Long, motivo: MotivoOmision, comentario: String?)
    suspend fun sembrarSiVacio()
}

interface SesionRepository {
    val rolActual: Flow<Rol?>
    suspend fun seleccionarRol(rol: Rol)
}

interface PedidoRepository {
    fun observarPedidos(): Flow<List<Pedido>>
    fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>>
    suspend fun crearPedido(puntoId: Long, litros: Int): Pedido
    suspend fun cancelarPedido(pedidoId: Long)
    suspend fun validarPunto(puntoId: Long)
}

interface TrackingRepository {
    fun observarUltimaPosicion(): Flow<PosicionCisterna?>
    fun observarPuntosDeZona(): Flow<List<PuntoAbastecimiento>>  // RV-02, solo lectura
}
