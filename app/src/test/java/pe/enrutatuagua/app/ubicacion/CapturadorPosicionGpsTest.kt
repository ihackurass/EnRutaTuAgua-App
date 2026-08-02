package pe.enrutatuagua.app.ubicacion

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CapturadorPosicionGpsTest {

    private lateinit var contexto: Application
    private lateinit var gestor: LocationManager

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        shadowOf(contexto).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        gestor = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowOf(gestor).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
    }

    private fun fix(lat: Double, lng: Double, velocidad: Float? = null) =
        Location(LocationManager.GPS_PROVIDER).apply {
            latitude = lat; longitude = lng
            if (velocidad != null) speed = velocidad
        }

    @Test fun `entrega los fixes del proveedor gps al callback`() {
        val capturador = CapturadorPosicionGps(contexto)
        val recibidas = mutableListOf<Location>()

        capturador.iniciar { recibidas += it }
        shadowOf(gestor).simulateLocation(fix(-12.1571, -76.9442))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, recibidas.size)
        assertEquals(-12.1571, recibidas[0].latitude, 1e-6)
    }

    @Test fun `detener deja de entregar fixes`() {
        val capturador = CapturadorPosicionGps(contexto)
        val recibidas = mutableListOf<Location>()
        capturador.iniciar { recibidas += it }
        capturador.detener()

        shadowOf(gestor).simulateLocation(fix(-12.1571, -76.9442))
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(recibidas.isEmpty())
    }

    @Test fun `un fix en movimiento ajusta el intervalo a 5s`() {
        val capturador = CapturadorPosicionGps(contexto)
        capturador.iniciar { }

        shadowOf(gestor).simulateLocation(fix(-12.1571, -76.9442, velocidad = 2.0f))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(5_000L, capturador.intervaloActualMs)
    }

    @Test fun `un fix sin velocidad o detenido ajusta el intervalo a 20s`() {
        val capturador = CapturadorPosicionGps(contexto)
        capturador.iniciar { }

        shadowOf(gestor).simulateLocation(fix(-12.1571, -76.9442, velocidad = 0.1f))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(20_000L, capturador.intervaloActualMs)
    }
}
