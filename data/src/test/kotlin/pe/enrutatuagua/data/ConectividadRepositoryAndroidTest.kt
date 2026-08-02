package pe.enrutatuagua.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConectividadRepositoryAndroidTest {
    private lateinit var contexto: Context
    private lateinit var cm: ConnectivityManager
    private lateinit var repo: ConectividadRepositoryAndroid

    @Before fun setUp() {
        contexto = ApplicationProvider.getApplicationContext()
        cm = contexto.getSystemService(ConnectivityManager::class.java)
        repo = ConectividadRepositoryAndroid(contexto)
    }

    @Test fun `sin red activa el primer valor es false`() = runTest {
        shadowOf(cm).setDefaultNetworkActive(false)

        val dispatcher = StandardTestDispatcher(testScheduler)
        val primero = withContext(dispatcher) {
            repo.observarConectividad().take(1).toList()
        }

        assertEquals(listOf(false), primero)
    }

    @Test fun `con red validada el primer valor es true`() = runTest {
        shadowOf(cm).setDefaultNetworkActive(true)
        val red = requireNotNull(cm.activeNetwork)
        val capacidades = ShadowNetworkCapabilities.newInstance()
        shadowOf(capacidades).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(capacidades).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadowOf(cm).setNetworkCapabilities(red, capacidades)

        val dispatcher = StandardTestDispatcher(testScheduler)
        val primero = withContext(dispatcher) {
            repo.observarConectividad().take(1).toList()
        }

        assertEquals(listOf(true), primero)
    }
}
