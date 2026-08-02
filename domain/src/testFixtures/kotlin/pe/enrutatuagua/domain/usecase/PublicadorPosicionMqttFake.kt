package pe.enrutatuagua.domain.usecase

import pe.enrutatuagua.domain.repositorio.PosicionGps
import pe.enrutatuagua.domain.repositorio.PublicadorPosicionMqtt

class PublicadorPosicionMqttFake : PublicadorPosicionMqtt {
    val publicadas = mutableListOf<Pair<Long, PosicionGps>>()
    var proximoResultado: Boolean = true

    override suspend fun publicar(aguateroId: Long, posicion: PosicionGps): Boolean {
        val exito = proximoResultado
        if (exito) publicadas += aguateroId to posicion
        return exito
    }
}
