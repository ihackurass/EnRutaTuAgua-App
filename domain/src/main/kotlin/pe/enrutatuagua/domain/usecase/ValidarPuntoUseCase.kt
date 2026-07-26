package pe.enrutatuagua.domain.usecase

import kotlinx.coroutines.flow.first
import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.EstadoValidacionPunto
import pe.enrutatuagua.domain.repositorio.PedidoRepository
import javax.inject.Inject

// RP-02, alcance estricto: solo aprueba puntos PROPUESTOs (ver Notas del plan).
class ValidarPuntoUseCase @Inject constructor(private val repo: PedidoRepository) {
    suspend operator fun invoke(puntoId: Long) {
        val punto = repo.observarPuntosDeZona().first().firstOrNull { it.id == puntoId }
            ?: throw ErrorDominio.PuntoNoValidable()
        if (punto.estado != EstadoValidacionPunto.PROPUESTO) throw ErrorDominio.PuntoNoValidable()
        repo.validarPunto(puntoId)
    }
}
