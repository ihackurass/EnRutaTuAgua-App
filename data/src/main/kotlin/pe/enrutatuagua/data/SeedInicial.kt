package pe.enrutatuagua.data

import pe.enrutatuagua.data.local.*

object SeedInicial {
    fun ruta(fecha: String): Triple<RutaEntity, List<PuntoEntity>, List<ParadaEntity>> {
        val puntos = listOf(
            PuntoEntity(1, "Comité 12 — Loma Alta", -12.1571, -76.9442, 4000),
            PuntoEntity(2, "AAHH Buena Vista, sector B", -12.1613, -76.9488, 3000),
            PuntoEntity(3, "Comité 7 — José C. Mariátegui", -12.1652, -76.9521, 6000),
            PuntoEntity(4, "Paradero Piedra Grande", -12.1699, -76.9563, 2500),
        )
        val ruta = RutaEntity(100, fecha, estadoCodigo = 1, vehiculoId = 5, vehiculoPlaca = "ABC-123", vehiculoCapacidadL = 12000)
        val paradas = puntos.mapIndexed { i, p ->
            ParadaEntity(id = 10L + i, rutaId = ruta.id, puntoId = p.id, orden = i + 1,
                capacidadEsperadaL = p.capacidadLitros, estadoCodigo = 1, motivoCodigo = null, comentario = null)
        }
        return Triple(ruta, puntos, paradas)
    }
}
