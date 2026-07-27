package pe.enrutatuagua.app.ubicacion

import android.app.PendingIntent
import android.location.Location
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MotorPosicionConRumboTest {

    /** Motor fake: captura el callback registrado y permite emitir fixes a mano. */
    private class MotorFake : LocationEngine {
        var callbackRegistrado: LocationEngineCallback<LocationEngineResult>? = null

        override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) = Unit

        override fun requestLocationUpdates(
            request: LocationEngineRequest,
            callback: LocationEngineCallback<LocationEngineResult>,
            looper: Looper?,
        ) {
            callbackRegistrado = callback
        }

        override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) = Unit

        override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
            if (callbackRegistrado == callback) callbackRegistrado = null
        }

        override fun removeLocationUpdates(pendingIntent: PendingIntent?) = Unit

        fun emitir(posicion: Location) {
            callbackRegistrado!!.onSuccess(LocationEngineResult.create(posicion))
        }
    }

    private class Receptor : LocationEngineCallback<LocationEngineResult> {
        val recibidas = mutableListOf<Location>()
        override fun onSuccess(resultado: LocationEngineResult) {
            resultado.lastLocation?.let { recibidas += it }
        }

        override fun onFailure(excepcion: Exception) = Unit
    }

    private fun posicion(lat: Double, lng: Double, rumbo: Float? = null) =
        Location("gps").apply {
            latitude = lat
            longitude = lng
            if (rumbo != null) bearing = rumbo
        }

    private fun motorSuscrito(fake: MotorFake, receptor: Receptor): MotorPosicionConRumbo {
        val motor = MotorPosicionConRumbo(fake)
        motor.requestLocationUpdates(LocationEngineRequest.Builder(1000L).build(), receptor, null)
        return motor
    }

    @Test
    fun `el primer fix sin rumbo pasa tal cual`() {
        val fake = MotorFake()
        val receptor = Receptor()
        motorSuscrito(fake, receptor)

        fake.emitir(posicion(-12.1571, -76.9442))

        assertEquals(1, receptor.recibidas.size)
        assertFalse(receptor.recibidas[0].hasBearing())
    }

    @Test
    fun `el segundo fix a mas de 2 metros gana el rumbo calculado`() {
        val fake = MotorFake()
        val receptor = Receptor()
        motorSuscrito(fake, receptor)

        fake.emitir(posicion(-12.1571, -76.9442))
        fake.emitir(posicion(-12.1613, -76.9488))

        val segunda = receptor.recibidas[1]
        assertTrue(segunda.hasBearing())
        assertEquals(rumboEntre(-12.1571, -76.9442, -12.1613, -76.9488), segunda.bearing, 1f)
    }

    @Test
    fun `un fix que ya trae rumbo pasa intacto`() {
        val fake = MotorFake()
        val receptor = Receptor()
        motorSuscrito(fake, receptor)

        fake.emitir(posicion(-12.1571, -76.9442))
        fake.emitir(posicion(-12.1613, -76.9488, rumbo = 77f))

        assertEquals(77f, receptor.recibidas[1].bearing, 0.01f)
    }

    @Test
    fun `detenido conserva el ultimo rumbo calculado`() {
        val fake = MotorFake()
        val receptor = Receptor()
        motorSuscrito(fake, receptor)

        fake.emitir(posicion(-12.1571, -76.9442))
        fake.emitir(posicion(-12.1613, -76.9488))
        val rumboEnMarcha = receptor.recibidas[1].bearing
        // ~0.5 m del fix anterior: bajo el umbral, no se recalcula
        fake.emitir(posicion(-12.161304, -76.948804))

        val detenida = receptor.recibidas[2]
        assertTrue(detenida.hasBearing())
        assertEquals(rumboEnMarcha, detenida.bearing, 0.01f)
    }

    @Test
    fun `removeLocationUpdates con el callback original desregistra la envoltura`() {
        val fake = MotorFake()
        val receptor = Receptor()
        val motor = motorSuscrito(fake, receptor)

        motor.removeLocationUpdates(receptor)

        assertNull(fake.callbackRegistrado)
    }
}
