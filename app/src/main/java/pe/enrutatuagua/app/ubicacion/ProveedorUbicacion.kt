package pe.enrutatuagua.app.ubicacion

import android.location.LocationManager

internal fun LocationManager.proveedorDisponible(): String = when {
    isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
    isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
    else -> LocationManager.PASSIVE_PROVIDER
}
