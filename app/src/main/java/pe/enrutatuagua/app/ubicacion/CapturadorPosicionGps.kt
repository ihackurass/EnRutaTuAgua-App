package pe.enrutatuagua.app.ubicacion

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper

private const val INTERVALO_MOVIMIENTO_MS = 5_000L
private const val INTERVALO_DETENIDO_MS = 20_000L

/** Mismo umbral que MotorPosicionConRumbo (plan 09): evita jitter de intervalo por ruido del fix. */
private const val VELOCIDAD_MINIMA_MOVIMIENTO = 0.5f

/**
 * Captura de GPS directa sobre LocationManager para un contexto headless
 * (sin mapa que renderizar) -- a diferencia de MotorPosicionGps (plan 09,
 * implementa la interfaz LocationEngine de MapLibre para alimentar el
 * LocationComponent), este captador no necesita esa indirección: sólo
 * entrega fixes crudos a un callback. Intervalo adaptativo (ANDROID-PLAN-04,
 * decisión 4): 5s en movimiento, 20s detenido -- se re-registra el listener
 * cada vez que cambia el régimen.
 */
class CapturadorPosicionGps(contexto: Context) {

    private val gestor =
        contexto.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    var intervaloActualMs: Long = INTERVALO_MOVIMIENTO_MS
        private set

    private var escuchaActiva: LocationListener? = null

    fun iniciar(onFix: (Location) -> Unit) {
        detener()
        val escucha = LocationListener { ubicacion ->
            onFix(ubicacion)
            reajustarIntervaloSiCambio(ubicacion)
        }
        escuchaActiva = escucha
        registrar(escucha, intervaloActualMs)
    }

    fun detener() {
        escuchaActiva?.let(gestor::removeUpdates)
        escuchaActiva = null
    }

    private fun reajustarIntervaloSiCambio(ubicacion: Location) {
        val nuevoIntervalo =
            if (ubicacion.hasSpeed() && ubicacion.speed > VELOCIDAD_MINIMA_MOVIMIENTO) INTERVALO_MOVIMIENTO_MS
            else INTERVALO_DETENIDO_MS
        if (nuevoIntervalo == intervaloActualMs) return
        intervaloActualMs = nuevoIntervalo
        val escucha = escuchaActiva ?: return
        gestor.removeUpdates(escucha)
        registrar(escucha, intervaloActualMs)
    }

    @SuppressLint("MissingPermission") // el llamador (ServicioTrackingGps) verifica el permiso antes de iniciar
    private fun registrar(escucha: LocationListener, intervaloMs: Long) {
        gestor.requestLocationUpdates(gestor.proveedorDisponible(), intervaloMs, 0f, escucha, Looper.getMainLooper())
    }
}
