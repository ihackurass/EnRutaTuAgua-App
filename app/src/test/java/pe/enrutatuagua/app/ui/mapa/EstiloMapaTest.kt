package pe.enrutatuagua.app.ui.mapa

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EstiloMapaTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `el estilo claro parsea y no deja marcador`() {
        val json = EstiloMapa.json(contexto, oscuro = false)
        assertFalse(json.contains("__PMTILES__"))
        val raiz = JSONObject(json)
        assertEquals(8, raiz.getInt("version"))
        assertTrue(raiz.getJSONObject("sources").has("protomaps"))
        assertTrue(json.contains("pmtiles://file://"))
    }

    @Test
    fun `la copia del pmtiles es idempotente`() {
        val primera = EstiloMapa.prepararPmtiles(contexto)
        val marca = primera.lastModified()
        val segunda = EstiloMapa.prepararPmtiles(contexto)
        assertEquals(primera.absolutePath, segunda.absolutePath)
        assertEquals(marca, segunda.lastModified())
    }
}
