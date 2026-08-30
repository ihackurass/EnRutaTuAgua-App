package pe.enrutatuagua.data.red

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.modelo.EstadoRuta
import pe.enrutatuagua.domain.modelo.MotivoOmision
import pe.enrutatuagua.domain.modelo.Punto
import pe.enrutatuagua.domain.modelo.Vehiculo
import java.time.LocalDate

class MappersRedTest {

    @Test fun `VehiculoDto se mapea directo al dominio`() {
        val dto = VehiculoDto(id = 5, placa = "ABC-123", capacidadTanqueLitros = 12_000)

        assertEquals(Vehiculo(5, "ABC-123", 12_000), dto.aDominio())
    }

    @Test fun `PuntoDto se mapea directo al dominio`() {
        val dto = PuntoDto(id = 1, nombre = "Comité 12", lat = -12.16, lng = -76.94, capacidadLitros = 4000)

        assertEquals(Punto(1, "Comité 12", -12.16, -76.94, 4000), dto.aDominio())
    }

    @Test fun `ParadaDto omitida mapea motivo y comentario`() {
        val dto = ParadaDto(
            id = 10, puntoId = 1, orden = 1, capacidadEsperadaLitros = 500,
            estadoEntrega = "OMITIDO", motivoOmision = "NADIE_PRESENTE", comentarioOmision = null,
        )
        val punto = Punto(1, "Comité 12", -12.16, -76.94, 4000)

        val parada = dto.aDominio(punto)

        assertEquals(EstadoEntrega.OMITIDO, parada.estadoEntrega)
        assertEquals(MotivoOmision.NADIE_PRESENTE, parada.motivoOmision)
        assertEquals(punto, parada.punto)
    }

    @Test fun `RutaDto enriquecida arma la Ruta completa`() {
        val paradaDto = ParadaDto(
            id = 10, puntoId = 1, orden = 1, capacidadEsperadaLitros = 500,
            estadoEntrega = "PENDIENTE", motivoOmision = null, comentarioOmision = null,
        )
        val rutaDto = RutaDto(
            id = 29, aguateroId = 3, fecha = "2026-08-02", estado = "ABIERTA",
            vehiculoId = 1, paradas = listOf(paradaDto), cerradaEn = null,
        )
        val vehiculo = Vehiculo(1, "ABC-123", 12_000)
        val punto = Punto(1, "Comité 12", -12.16, -76.94, 4000)

        val ruta = rutaDto.aDominio(vehiculo, mapOf(1L to punto))

        assertEquals(29L, ruta.id)
        assertEquals(LocalDate.parse("2026-08-02"), ruta.fecha)
        assertEquals(EstadoRuta.ABIERTA, ruta.estado)
        assertEquals(vehiculo, ruta.vehiculo)
        assertEquals(1, ruta.paradas.size)
        assertEquals(punto, ruta.paradas[0].punto)
    }

    @Test fun `entregasAproximadas suma solo las paradas ENTREGADO`() {
        val paradaEntregada = ParadaDto(
            id = 1, puntoId = 1, orden = 1, capacidadEsperadaLitros = 500,
            litrosEntregados = 500, estadoEntrega = "ENTREGADO",
        )
        val paradaPendiente = ParadaDto(
            id = 2, puntoId = 2, orden = 2, capacidadEsperadaLitros = 300, estadoEntrega = "PENDIENTE",
        )
        val punto = Punto(1, "x", 0.0, 0.0, 0)
        val ruta = RutaDto(29, 3, "2026-08-02", "ABIERTA", 1, listOf(paradaEntregada, paradaPendiente), null)
            .aDominio(Vehiculo(1, "ABC-123", 12_000), mapOf(1L to punto, 2L to punto))

        val entregas = ruta.entregasAproximadas()

        assertEquals(1, entregas.size)
        assertEquals(500, entregas[0].litros)
    }
}
