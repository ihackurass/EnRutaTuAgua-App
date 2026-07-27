package pe.enrutatuagua.app.ui.mapa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.usecase.RutaRepoFake

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
}
