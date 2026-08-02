package pe.enrutatuagua.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import pe.enrutatuagua.domain.repositorio.ConectividadRepository
import javax.inject.Inject

/**
 * ConnectivityManager.NetworkCallback envuelto en Flow<Boolean>. El
 * debounce va ACÁ (no en cada consumidor) para que tanto el switch de
 * estilo del mapa como el disparador de precarga reciban una señal ya
 * estabilizada -- sin esto, un corte de señal de un instante dispararía
 * reloads de estilo y descargas redundantes en cada lugar que observe.
 */
class ConectividadRepositoryAndroid @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : ConectividadRepository {

    @OptIn(FlowPreview::class)
    override fun observarConectividad(): Flow<Boolean> = callbackFlow {
        val cm = contexto.getSystemService(ConnectivityManager::class.java)
        trySend(hayInternetValidado(cm))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(red: Network) {
                trySend(hayInternetValidado(cm))
            }

            override fun onLost(red: Network) {
                trySend(hayInternetValidado(cm))
            }

            override fun onCapabilitiesChanged(red: Network, capacidades: NetworkCapabilities) {
                trySend(hayInternetValidado(cm))
            }
        }
        val solicitud = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(solicitud, callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().debounce(DEBOUNCE_MILLIS)

    private fun hayInternetValidado(cm: ConnectivityManager): Boolean {
        val red = cm.activeNetwork ?: return false
        val capacidades = cm.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 3_000L
    }
}
