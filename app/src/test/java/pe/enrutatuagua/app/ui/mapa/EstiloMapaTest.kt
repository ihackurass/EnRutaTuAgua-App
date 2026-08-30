package pe.enrutatuagua.app.ui.mapa

import org.junit.Assert.assertEquals
import org.junit.Test

class EstiloMapaTest {
    @Test fun `usa el estilo oficial estable de OpenFreeMap`() {
        assertEquals("https://tiles.openfreemap.org/styles/liberty", EstiloMapa.URL_ONLINE)
    }
}
