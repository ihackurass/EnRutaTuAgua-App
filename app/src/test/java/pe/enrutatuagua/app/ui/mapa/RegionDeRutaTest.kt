package pe.enrutatuagua.app.ui.mapa

import org.junit.Assert.assertEquals
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

class RegionDeRutaTest {

    @Test
    fun `sin paradas no hay region que precargar`() {
        val ruta = RutaRepoFake.rutaDePrueba().copy(paradas = emptyList())
        assertNull(regionDeRuta(ruta))
    }

    @Test
    fun `una sola parada da un bbox centrado en ese punto mas el margen`() {
        val ruta = RutaRepoFake.rutaDePrueba() // punto único: -12.1571, -76.9442
        val region = regionDeRuta(ruta, margenGrados = 0.03)

        assertEquals(-12.1571 + 0.03, region!!.latNorte, 1e-9)
        assertEquals(-12.1571 - 0.03, region.latSur, 1e-9)
        assertEquals(-76.9442 + 0.03, region.lonEste, 1e-9)
        assertEquals(-76.9442 - 0.03, region.lonOeste, 1e-9)
    }

    @Test
    fun `varias paradas dan el bbox envolvente mas el margen`() {
        val vehiculo = Vehiculo(5, "ABC-123", 12000)
        val paradas = listOf(
            Parada(1, Punto(1, "A", -12.10, -76.95, 1000), 1, 1000, EstadoEntrega.PENDIENTE, null, null),
            Parada(2, Punto(2, "B", -12.20, -76.90, 1000), 2, 1000, EstadoEntrega.PENDIENTE, null, null),
        )
        val ruta = Ruta(1, LocalDate.parse("2026-07-25"), EstadoRuta.ABIERTA, vehiculo, paradas)

        val region = regionDeRuta(ruta, margenGrados = 0.03)

        assertEquals(-12.10 + 0.03, region!!.latNorte, 1e-9)
        assertEquals(-12.20 - 0.03, region.latSur, 1e-9)
        assertEquals(-76.90 + 0.03, region.lonEste, 1e-9)
        assertEquals(-76.95 - 0.03, region.lonOeste, 1e-9)
    }
}
