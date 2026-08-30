package pe.enrutatuagua.app.ubicacion

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class AnclaTracking(val lat: Double, val lng: Double, val obtenidaEnMs: Long)

sealed interface ResultadoAnclaTracking {
    data class Disponible(val ancla: AnclaTracking) : ResultadoAnclaTracking
    data object SinPermiso : ResultadoAnclaTracking
    data object UbicacionDesactivada : ResultadoAnclaTracking
    data object SinFixReciente : ResultadoAnclaTracking
}

interface ProveedorAnclaTracking {
    suspend fun obtener(): ResultadoAnclaTracking
}

/** Obtiene un fix actual y sólo usa el último conocido si todavía es reciente. */
class ProveedorAnclaTrackingAndroid @Inject constructor(
    @ApplicationContext contexto: Context,
) : ProveedorAnclaTracking {
    private val contextoAplicacion = contexto.applicationContext
    private val gestor = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override suspend fun obtener(): ResultadoAnclaTracking {
        if (ContextCompat.checkSelfPermission(
                contextoAplicacion, Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) return ResultadoAnclaTracking.SinPermiso

        // "fused" suele entregar rápidamente una ubicación Wi-Fi/celda en interiores;
        // GPS y network quedan como fuentes paralelas y respaldo.
        val proveedores = listOf("fused", LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { gestor.isProviderEnabled(it) }.getOrDefault(false) }
        if (proveedores.isEmpty()) return ResultadoAnclaTracking.UbicacionDesactivada

        val actual = withTimeoutOrNull(TIMEOUT_FIX_MS) { esperarFix(proveedores) }
        val elegida = actual ?: ultimaConocidaReciente(proveedores)
            ?: return ResultadoAnclaTracking.SinFixReciente
        return ResultadoAnclaTracking.Disponible(
            AnclaTracking(elegida.latitude, elegida.longitude, elegida.time),
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun esperarFix(proveedores: List<String>): Location? = suspendCancellableCoroutine { continuacion ->
        val escucha = object : LocationListener {
            override fun onLocationChanged(ubicacion: Location) {
                gestor.removeUpdates(this)
                if (continuacion.isActive) continuacion.resume(ubicacion)
            }
        }
        proveedores.forEach { proveedor ->
            gestor.requestLocationUpdates(proveedor, 0L, 0f, escucha, Looper.getMainLooper())
        }
        continuacion.invokeOnCancellation { gestor.removeUpdates(escucha) }
    }

    @SuppressLint("MissingPermission")
    private fun ultimaConocidaReciente(proveedores: List<String>): Location? {
        val ahora = System.currentTimeMillis()
        return proveedores.mapNotNull { runCatching { gestor.getLastKnownLocation(it) }.getOrNull() }
            .filter { ahora - it.time in 0..MAX_ANTIGUEDAD_MS }
            .maxByOrNull(Location::getTime)
    }

    private companion object {
        const val TIMEOUT_FIX_MS = 15_000L
        const val MAX_ANTIGUEDAD_MS = 120_000L
    }
}
