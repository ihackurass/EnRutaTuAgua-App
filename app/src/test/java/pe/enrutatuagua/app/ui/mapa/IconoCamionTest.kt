package pe.enrutatuagua.app.ui.mapa

import android.app.Application
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.app.R

@RunWith(RobolectricTestRunner::class)
class IconoCamionTest {

    @Test
    fun `el icono del camion existe y es cuadrado`() {
        val contexto = ApplicationProvider.getApplicationContext<Application>()
        val icono = ResourcesCompat.getDrawable(contexto.resources, R.drawable.ic_camion_cisterna, null)

        assertNotNull(icono)
        // El puck rota el drawable con el rumbo: un asset no cuadrado se recorta al girar.
        assertEquals(icono!!.intrinsicWidth, icono.intrinsicHeight)
    }
}
