package pe.enrutatuagua.data

import pe.enrutatuagua.data.local.*
import pe.enrutatuagua.domain.modelo.*
import java.time.LocalDate

fun estadoRutaDesdeCodigo(c: Int) = EstadoRuta.entries.first { it.codigo == c }
fun estadoEntregaDesdeCodigo(c: Int) = EstadoEntrega.entries.first { it.codigo == c }
fun motivoDesdeCodigo(c: Int?) = c?.let { cod -> MotivoOmision.entries.first { it.codigo == cod } }
fun tipoDesdeCodigo(c: Int) = TipoEntrega.entries.first { it.codigo == c }

fun RutaCompleta.aDominio() = Ruta(
    id = ruta.id,
    fecha = LocalDate.parse(ruta.fecha),
    estado = estadoRutaDesdeCodigo(ruta.estadoCodigo),
    vehiculo = Vehiculo(ruta.vehiculoId, ruta.vehiculoPlaca, ruta.vehiculoCapacidadL),
    paradas = paradas.sortedBy { it.parada.orden }.map { pc ->
        Parada(
            id = pc.parada.id,
            punto = Punto(pc.punto.id, pc.punto.nombre, pc.punto.lat, pc.punto.lng, pc.punto.capacidadLitros),
            orden = pc.parada.orden,
            capacidadEsperadaLitros = pc.parada.capacidadEsperadaL,
            estadoEntrega = estadoEntregaDesdeCodigo(pc.parada.estadoCodigo),
            motivoOmision = motivoDesdeCodigo(pc.parada.motivoCodigo),
            comentarioOmision = pc.parada.comentario,
        )
    },
)

fun EntregaEntity.aDominio() = Entrega(id, paradaId, tipoDesdeCodigo(tipoCodigo), ajustaA, litros, gpsLat, gpsLng, registradaEnMillis)
fun Entrega.aEntity() = EntregaEntity(id, paradaId, tipo.codigo, ajustaA, litros, gpsLat, gpsLng, registradaEn)
