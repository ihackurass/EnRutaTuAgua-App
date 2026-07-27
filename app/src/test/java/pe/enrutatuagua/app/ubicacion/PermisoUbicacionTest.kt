package pe.enrutatuagua.app.ubicacion

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PermisoUbicacionTest {

    @get:Rule
    val regla = createComposeRule()

    @Test
    fun `con el permiso ya concedido arranca en concedido`() {
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        var estado: EstadoPermisoUbicacion? = null
        regla.setContent { estado = rememberPermisoUbicacion() }

        assertTrue(estado!!.concedido)
    }

    @Test
    fun `sin permiso arranca en no concedido y solicitar no revienta`() {
        var estado: EstadoPermisoUbicacion? = null
        regla.setContent { estado = rememberPermisoUbicacion() }

        assertFalse(estado!!.concedido)
        regla.runOnIdle { estado!!.solicitar() }
    }
}
