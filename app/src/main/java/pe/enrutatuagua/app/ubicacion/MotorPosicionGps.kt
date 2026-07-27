package pe.enrutatuagua.app.ubicacion

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

/**
 * Motor de posición directo sobre LocationManager, prefiriendo el proveedor GPS.
 *
 * ¿Por qué no LocationEngineDefault? En API 31+ su `getBestProvider` devuelve
 * siempre "fused" y el request queda con calidad BALANCED: la GNSS ni se
 * enciende y la posición sale de la red (en el emulador, `adb emu geo fix` se
 * ignora y llega la ubicación por defecto de Google). En campo pasa lo mismo:
 * sin wifi ni celular, el GPS es el único proveedor útil — registrarlo directo
 * enciende la GNSS.
 */
class MotorPosicionGps(contexto: Context) : LocationEngine {

    private val gestor =
        contexto.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val escuchas =
        mutableMapOf<LocationEngineCallback<LocationEngineResult>, LocationListener>()

    private fun proveedor(): String = when {
        gestor.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        gestor.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> LocationManager.PASSIVE_PROVIDER
    }

    @SuppressLint("MissingPermission") // MapaRuta activa el puck solo con permiso concedido
    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        val ultima: Location? = gestor.getLastKnownLocation(proveedor())
        if (ultima != null) {
            callback.onSuccess(LocationEngineResult.create(ultima))
        } else {
            callback.onFailure(Exception("Sin última posición conocida"))
        }
    }

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?,
    ) {
        val escucha = escuchas.getOrPut(callback) {
            LocationListener { posicion -> callback.onSuccess(LocationEngineResult.create(posicion)) }
        }
        gestor.requestLocationUpdates(
            proveedor(),
            request.interval,
            request.displacement,
            escucha,
            looper ?: Looper.getMainLooper(),
        )
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) {
        // El LocationComponent no usa la variante con PendingIntent.
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        escuchas.remove(callback)?.let(gestor::removeUpdates)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) = Unit
}
