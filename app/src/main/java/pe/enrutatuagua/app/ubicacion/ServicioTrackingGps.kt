package pe.enrutatuagua.app.ubicacion

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

/**
 * Wrapper delgado sobre MotorPublicacionGps -- toda la lógica real (guardar,
 * publicar, drenar) vive ahí, testeable sin el ciclo de vida de Android
 * (mismo criterio que los ViewModels de este repo delegando a use cases).
 * El tipo del foreground service (location) se declara en el manifest
 * (android:foregroundServiceType="location"), por eso startForeground()
 * usa el overload de 2 argumentos -- válido en todas las API levels desde
 * minSdk 26, sin ramificar por versión.
 */
@AndroidEntryPoint
class ServicioTrackingGps : Service() {

    @Inject lateinit var motor: MotorPublicacionGps

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Defensivo: ObservadorRutaAbiertaGps ya no debería arrancar el
        // service sin el permiso concedido, pero un foreground service con
        // tipo location SIN el permiso tira SecurityException y crashea la
        // app (hallazgo real en dispositivo) -- si por cualquier otro
        // camino (p.ej. un restart START_STICKY del sistema tras revocar el
        // permiso) llega acá sin permiso, se apaga solo en vez de crashear.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }
        NotificacionTrackingGps.crearCanalSiHaceFalta(this)
        startForeground(NotificacionTrackingGps.id(), NotificacionTrackingGps.construir(this))
        motor.iniciar(this, alcance)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        motor.detener()
        alcance.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
