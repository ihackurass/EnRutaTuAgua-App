package pe.enrutatuagua.data

import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pe.enrutatuagua.domain.modelo.EstadoRuta
import pe.enrutatuagua.domain.repositorio.AutenticacionRepository
import pe.enrutatuagua.domain.repositorio.ContextoTracking
import pe.enrutatuagua.domain.repositorio.ContextoTrackingRepository
import pe.enrutatuagua.domain.repositorio.RutaRepository

@Singleton
class ContextoTrackingRepositoryRuntime @Inject constructor(
    private val autenticacion: AutenticacionRepository,
    private val rutas: RutaRepository,
) : ContextoTrackingRepository {
    override suspend fun actual(): ContextoTracking? {
        val ruta = rutas.observarRutaAbierta().first() ?: return null
        if (ruta.estado != EstadoRuta.ABIERTA) return null
        val payload = autenticacion.tokenVigente().split('.').getOrNull(1) ?: return null
        val json = runCatching {
            String(Base64.getUrlDecoder().decode(payload.padEnd((payload.length + 3) / 4 * 4, '=')))
        }.getOrNull() ?: return null
        val aguateroId = runCatching {
            Json.parseToJsonElement(json).jsonObject.getValue("sub").jsonPrimitive.content.toLong()
        }.getOrNull() ?: return null
        return ContextoTracking(aguateroId, ruta.id)
    }
}
