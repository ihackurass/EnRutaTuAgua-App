package pe.enrutatuagua.app.ubicacion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

private const val CANAL_ID = "tracking_gps"
private const val NOTIFICACION_ID = 1001

/**
 * Notificación persistente exigida por RA-07 mientras el foreground service
 * de rastreo GPS corre. Ícono stock de Android (deuda: reemplazar por uno
 * propio del design system, ver ANDROID-PLAN-04 "Deuda").
 */
object NotificacionTrackingGps {

    fun id(): Int = NOTIFICACION_ID

    fun crearCanalSiHaceFalta(contexto: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val gestor = contexto.getSystemService(NotificationManager::class.java)
        if (gestor.getNotificationChannel(CANAL_ID) != null) return
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_ID, "Rastreo GPS", NotificationManager.IMPORTANCE_LOW),
        )
    }

    fun construir(contexto: Context): Notification =
        Notification.Builder(contexto, CANAL_ID)
            .setContentTitle("EnRutaTuAgua")
            .setContentText("Compartiendo tu ubicación mientras la ruta está abierta")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
}
