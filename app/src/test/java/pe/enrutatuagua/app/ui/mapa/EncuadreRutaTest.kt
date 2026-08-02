package pe.enrutatuagua.app.ui.mapa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.modelo.EstadoRuta
import pe.enrutatuagua.domain.modelo.Parada
import pe.enrutatuagua.domain.modelo.Punto
import pe.enrutatuagua.domain.modelo.Ruta
import pe.enrutatuagua.domain.modelo.Vehiculo
import pe.enrutatuagua.domain.usecase.RutaRepoFake
import java.time.LocalDate

class EncuadreRutaTest {

    @Test
    fun `la misma geometria da la misma clave`() {
        assertEquals(claveDeEncuadre(RutaRepoFake.rutaDePrueba()), claveDeEncuadre(RutaRepoFake.rutaDePrueba()))
    }

    @Test
    fun `cambiar una coordenada cambia la clave`() {
        val ruta = RutaRepoFake.rutaDePrueba()
        val parada = ruta.paradas[0]
        val movida = ruta.copy(
            paradas = listOf(parada.copy(punto = parada.punto.copy(lat = parada.punto.lat + 0.01))),
        )
        assertNotEquals(claveDeEncuadre(ruta), claveDeEncuadre(movida))
    }

    @Test
    fun `cambiar el estado de entrega no cambia la clave`() {
        val ruta = RutaRepoFake.rutaDePrueba()
        val entregada = ruta.copy(
            paradas = listOf(ruta.paradas[0].copy(estadoEntrega = EstadoEntrega.ENTREGADO)),
        )
        assertEquals(claveDeEncuadre(ruta), claveDeEncuadre(entregada))
    }

    @Test
    fun `sin dos paradas no hay bearing que calcular`() {
        assertNull(bearingLecturaIzqADer(RutaRepoFake.rutaDePrueba())) // una sola parada
        assertNull(bearingLecturaIzqADer(RutaRepoFake.rutaDePrueba().copy(paradas = emptyList())))
    }

    @Test
    fun `parada final al este (en el ecuador) da bearing 0 -- queda a la derecha sin rotar`() {
        val ruta = rutaDePrueba(
            Punto(1, "A", lat = 0.0, lng = -77.0, capacidadLitros = 1000) to 1,
            Punto(2, "B", lat = 0.0, lng = -76.0, capacidadLitros = 1000) to 2, // al este de A, misma latitud
        )
        assertEquals(0.0, bearingLecturaIzqADer(ruta)!!, 1e-6)
    }

    @Test
    fun `parada final al norte da bearing 270 -- hay que rotar para que quede a la derecha`() {
        val ruta = rutaDePrueba(
            Punto(1, "A", lat = -1.0, lng = -77.0, capacidadLitros = 1000) to 1,
            Punto(2, "B", lat = 1.0, lng = -77.0, capacidadLitros = 1000) to 2, // al norte de A, misma longitud
        )
        assertEquals(270.0, bearingLecturaIzqADer(ruta)!!, 1e-6)
    }

    @Test
    fun `usa la parada de menor orden como punto de partida, no la primera de la lista`() {
        val enOrden = rutaDePrueba(
            Punto(1, "A", lat = 0.0, lng = -77.0, capacidadLitros = 1000) to 1,
            Punto(2, "B", lat = 0.0, lng = -76.0, capacidadLitros = 1000) to 2,
        )
        val listaDadaVuelta = enOrden.copy(paradas = enOrden.paradas.reversed())
        assertEquals(bearingLecturaIzqADer(enOrden), bearingLecturaIzqADer(listaDadaVuelta))
    }

    private fun rutaDePrueba(vararg puntosConOrden: Pair<Punto, Int>): Ruta {
        val paradas = puntosConOrden.map { (punto, orden) ->
            Parada(orden.toLong(), punto, orden, punto.capacidadLitros, EstadoEntrega.PENDIENTE, null, null)
        }
        return Ruta(1, LocalDate.parse("2026-07-25"), EstadoRuta.ABIERTA, Vehiculo(1, "ABC-123", 12000), paradas)
    }
}
