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
