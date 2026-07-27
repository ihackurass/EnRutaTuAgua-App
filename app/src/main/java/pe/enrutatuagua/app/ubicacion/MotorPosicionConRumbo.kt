package pe.enrutatuagua.app.ubicacion

import android.app.PendingIntent
import android.location.Location
import android.os.Looper
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

/**
 * Decora un [LocationEngine] calculando el rumbo cuando el fix no lo trae:
 * `adb emu geo fix` (y algunos proveedores reales) emiten posiciones sin
 * bearing, y sin él RenderMode.GPS dejaría el camión mirando siempre al norte.
 * Bajo [UMBRAL_METROS] se conserva el último rumbo: un camión detenido no
 * debe vibrar por el ruido del GPS.
 */
class MotorPosicionConRumbo(private val delegado: LocationEngine) : LocationEngine {

    private var ultimaPosicion: Location? = null
    private var ultimoRumbo: Float? = null

    /**
     * Envoltura registrada por cada callback original: removeLocationUpdates
     * llega con el original y debe desregistrar en el delegado la MISMA
     * envoltura que se registró en su momento.
     */
    private val envolturas =
        mutableMapOf<LocationEngineCallback<LocationEngineResult>, LocationEngineCallback<LocationEngineResult>>()

    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) =
        delegado.getLastLocation(callback)

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?,
    ) {
        val envoltura = envolturas.getOrPut(callback) {
            object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(resultado: LocationEngineResult?) {
                    val posicion = resultado?.lastLocation ?: return
                    callback.onSuccess(LocationEngineResult.create(conRumbo(posicion)))
                }

                override fun onFailure(excepcion: Exception) = callback.onFailure(excepcion)
            }
        }
        delegado.requestLocationUpdates(request, envoltura, looper)
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) =
        delegado.requestLocationUpdates(request, pendingIntent)

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        delegado.removeLocationUpdates(envolturas.remove(callback) ?: callback)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) =
        delegado.removeLocationUpdates(pendingIntent)

    private fun conRumbo(posicion: Location): Location {
        if (!posicion.hasBearing()) {
            val previa = ultimaPosicion
            if (previa != null && previa.distanceTo(posicion) > UMBRAL_METROS) {
                posicion.bearing =
                    rumboEntre(previa.latitude, previa.longitude, posicion.latitude, posicion.longitude)
            } else {
                ultimoRumbo?.let { posicion.bearing = it }
            }
        }
        if (posicion.hasBearing()) ultimoRumbo = posicion.bearing
        ultimaPosicion = posicion
        return posicion
    }

    private companion object {
        const val UMBRAL_METROS = 2f
    }
}
