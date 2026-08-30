package pe.enrutatuagua.app.ui.vecino

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.domain.usecase.PedidoRepoFake
import pe.enrutatuagua.domain.usecase.TrackingRepoFake

@RunWith(RobolectricTestRunner::class)
class GeoJsonZonaVecinoTest {
    private val puntos = PedidoRepoFake.puntosDePrueba()
    private val posicion = TrackingRepoFake.posicionDePrueba(1_000_000_000L)

    @Test
    fun `un punto por cada punto de la zona mas un feature de cisterna`() {
        val features = JSONObject(geoJsonDeZona(puntos, posicion)).getJSONArray("features")
        assertEquals(puntos.size + 1, features.length())
    }

    @Test
    fun `sin posicion no hay feature de cisterna`() {
        val features = JSONObject(geoJsonDeZona(puntos, null)).getJSONArray("features")
        assertEquals(puntos.size, features.length())
    }

    @Test
    fun `cada punto lleva su nombre y estado como propiedades`() {
        val features = JSONObject(geoJsonDeZona(puntos, posicion)).getJSONArray("features")
        val props = features.getJSONObject(0).getJSONObject("properties")
        assertEquals("punto", props.getString("tipo"))
        assertEquals(puntos[0].nombre, props.getString("nombre"))
        assertEquals(puntos[0].estado.name, props.getString("estado"))
    }

    @Test
    fun `el ultimo feature es la cisterna en su posicion`() {
        val features = JSONObject(geoJsonDeZona(puntos, posicion, 137f)).getJSONArray("features")
        val cisterna = features.getJSONObject(features.length() - 1)
        assertEquals("cisterna", cisterna.getJSONObject("properties").getString("tipo"))
        assertEquals(137.0, cisterna.getJSONObject("properties").getDouble("rumbo"), 0.01)
        val coords = cisterna.getJSONObject("geometry").getJSONArray("coordinates")
        assertEquals(posicion.lng, coords.getDouble(0), 0.0001)
        assertEquals(posicion.lat, coords.getDouble(1), 0.0001)
    }
}
