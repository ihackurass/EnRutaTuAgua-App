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
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MotorPosicionGpsTest {

    private lateinit var contexto: Application
    private lateinit var gestor: LocationManager

    private class Receptor : LocationEngineCallback<LocationEngineResult> {
        val recibidas = mutableListOf<Location>()
        override fun onSuccess(resultado: LocationEngineResult) {
            resultado.lastLocation?.let { recibidas += it }
        }

        override fun onFailure(excepcion: Exception) = Unit
    }

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        shadowOf(contexto).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        gestor = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowOf(gestor).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
    }

    private fun fix(lat: Double, lng: Double) =
        Location(LocationManager.GPS_PROVIDER).apply { latitude = lat; longitude = lng }

    @Test
    fun `entrega los fixes del proveedor gps al callback`() {
        val motor = MotorPosicionGps(contexto)
        val receptor = Receptor()
        motor.requestLocationUpdates(
            LocationEngineRequest.Builder(1000L).build(), receptor, Looper.getMainLooper(),
        )

        shadowOf(gestor).simulateLocation(fix(-12.1571, -76.9442))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, receptor.recibidas.size)
        assertEquals(-12.1571, receptor.recibidas[0].latitude, 1e-6)
    }

    @Test
    fun `removeLocationUpdates deja de entregar fixes`() {
        val motor = MotorPosicionGps(contexto)
        val receptor = Receptor()
        motor.requestLocationUpdates(
            LocationEngineRequest.Builder(1000L).build(), receptor, Looper.getMainLooper(),
        )
        motor.removeLocationUpdates(receptor)

        shadowOf(gestor).simulateLocation(fix(-12.1571, -76.9442))
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(receptor.recibidas.isEmpty())
    }
}
