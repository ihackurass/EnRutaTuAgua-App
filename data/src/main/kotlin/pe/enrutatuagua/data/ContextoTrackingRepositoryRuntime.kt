package pe.enrutatuagua.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import pe.enrutatuagua.domain.modelo.EstadoRuta
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.repositorio.ContextoTracking
import pe.enrutatuagua.domain.repositorio.ContextoTrackingRepository
import pe.enrutatuagua.domain.repositorio.RutaRepository
import pe.enrutatuagua.domain.repositorio.SesionRepository

@Singleton
class ContextoTrackingRepositoryRuntime @Inject constructor(
    private val sesion: SesionRepository,
    private val rutas: RutaRepository,
) : ContextoTrackingRepository {
    override suspend fun actual(): ContextoTracking? {
        val ruta = rutas.observarRutaAbierta().first() ?: return null
        if (ruta.estado != EstadoRuta.ABIERTA) return null
        val actual = sesion.sesion.first() ?: return null
        if (actual.rolActivo != Rol.AGUATERO) return null
        return ContextoTracking(actual.usuarioId, ruta.id)
    }
}
