package pe.enrutatuagua.app.servicio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import pe.enrutatuagua.app.ubicacion.ServicioTrackingGps
import pe.enrutatuagua.domain.modelo.EstadoRuta
import pe.enrutatuagua.domain.repositorio.RutaRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arranca/detiene ServicioTrackingGps observando
 * RutaRepository.observarRutaAbierta() (Room, sobrevive muerte de proceso)
 * -- no SesionRepository.rolActual (en memoria, se pierde). Sólo un
 * aguatero tiene datos de ruta sembrados hoy, así que "hay ruta ABIERTA" ya
 * implica "soy aguatero" (ANDROID-PLAN-04, decisión 2).
 *
 * Hallazgo real (crash en dispositivo): si la ruta ya está ABIERTA cuando
 * arranca la app (p.ej. sembrada de una corrida anterior), este observador
 * intentaba levantar el foreground service ANTES de que el usuario hubiera
 * concedido el permiso de ubicación -- `startForegroundService` con tipo
 * `location` sin `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` tira
 * `SecurityException` y crashea la app en loop (Android reintenta el
 * `START_STICKY`). El permiso se pide desde `RutaScreen`, pero puede
 * otorgarse por un camino que este observador no puede escuchar
 * directamente (p.ej. Ajustes del sistema) -- se resuelve reintentando cada
 * 2s hasta que el permiso aparece o la ruta se cierra, en vez de asumir que
 * ya está concedido.
 */
@Singleton
class ObservadorRutaAbiertaGps @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val rutaRepository: RutaRepository,
) {
    private var jobEspera: Job? = null

    fun iniciar(alcance: CoroutineScope) {
        rutaRepository.observarRutaAbierta()
            .map { it?.estado == EstadoRuta.ABIERTA }
            .distinctUntilChanged()
            .onEach { hayRutaAbierta -> reaccionar(hayRutaAbierta, alcance) }
            // Hallazgo real (crash sin ruta al backend): sin retry, un solo
            // SocketTimeoutException mataba esta colección para siempre --
            // el tracking GPS dejaba de reaccionar a abrir/cerrar ruta por
            // el resto de la sesión, en silencio. Reintenta cada 5s.
            .retry { delay(5_000L); true }
            .launchIn(alcance)
    }

    private fun reaccionar(hayRutaAbierta: Boolean, alcance: CoroutineScope) {
        jobEspera?.cancel()
        val intent = Intent(contexto, ServicioTrackingGps::class.java)
        if (!hayRutaAbierta) {
            contexto.stopService(intent)
            return
        }
        if (tienePermisoUbicacion()) {
            ContextCompat.startForegroundService(contexto, intent)
            return
        }
        jobEspera = alcance.launch {
            while (!tienePermisoUbicacion()) {
                delay(2_000L)
                if (rutaRepository.observarRutaAbierta().first()?.estado != EstadoRuta.ABIERTA) return@launch
            }
            ContextCompat.startForegroundService(contexto, intent)
        }
    }

    private fun tienePermisoUbicacion(): Boolean =
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
