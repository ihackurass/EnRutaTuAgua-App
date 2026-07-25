package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.flow.first
import pe.enrutatuagua.domain.modelo.*
import pe.enrutatuagua.domain.repositorio.RutaRepository
import javax.inject.Inject

class RegistrarEntregaUseCase(
    private val repo: RutaRepository,
    private val generarId: () -> String,          // inyectable: UUID en prod, fijo en tests (RN-02)
    private val ahora: () -> Long,
) {
    @Inject constructor(repo: RutaRepository) :
        this(repo, { java.util.UUID.randomUUID().toString() }, { System.currentTimeMillis() })

    suspend operator fun invoke(paradaId: Long, litros: Int) {
        if (litros <= 0) throw ErrorDominio.LitrosInvalidos()
        val ruta = repo.observarRutaAbierta().first() ?: throw ErrorDominio.SinRutaAbierta()
        val entregas = repo.observarEntregas(ruta.id).first()
        if (litros > litrosRestantes(ruta.vehiculo, entregas)) throw ErrorDominio.TanqueInsuficiente()
        repo.registrarEntrega(
            Entrega(
                id = generarId(), paradaId = paradaId, tipo = TipoEntrega.ENTREGA, ajustaA = null,
                litros = litros, gpsLat = null, gpsLng = null, registradaEn = ahora(),
            )
        )
    }
}
