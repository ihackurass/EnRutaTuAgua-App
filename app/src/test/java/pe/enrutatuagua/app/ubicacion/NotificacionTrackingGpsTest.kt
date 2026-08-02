package pe.enrutatuagua.app.ubicacion

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificacionTrackingGpsTest {

    @Test fun `construye una notificacion persistente valida`() {
        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        NotificacionTrackingGps.crearCanalSiHaceFalta(contexto)

        val notificacion = NotificacionTrackingGps.construir(contexto)

        assertTrue((notificacion.flags and Notification.FLAG_ONGOING_EVENT) != 0)
    }
}
