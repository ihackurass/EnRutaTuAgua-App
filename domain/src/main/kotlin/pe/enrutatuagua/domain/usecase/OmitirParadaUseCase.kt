package pe.enrutatuagua.domain.usecase

import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.MotivoOmision
import pe.enrutatuagua.domain.repositorio.RutaRepository
import javax.inject.Inject

class OmitirParadaUseCase @Inject constructor(private val repo: RutaRepository) {
    suspend operator fun invoke(paradaId: Long, motivo: MotivoOmision, comentario: String?) {
        if (motivo == MotivoOmision.OTRO && comentario.isNullOrBlank())
            throw ErrorDominio.ComentarioRequerido()
        repo.omitirParada(paradaId, motivo, comentario?.takeIf { it.isNotBlank() })
    }
}
