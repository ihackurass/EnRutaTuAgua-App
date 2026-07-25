package pe.enrutatuagua.app.ui.mapa

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.domain.usecase.RutaRepoFake

@RunWith(RobolectricTestRunner::class)
class GeoJsonRutaTest {
    @Test fun `linea mas un punto por parada`() {
        val ruta = RutaRepoFake.rutaDePrueba()
        val features = JSONObject(geoJsonDeRuta(ruta)).getJSONArray("features")
        assertEquals(1 + ruta.paradas.size, features.length())
        assertEquals("LineString", features.getJSONObject(0).getJSONObject("geometry").getString("type"))
    }
}
