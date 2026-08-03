package pe.enrutatuagua.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import pe.enrutatuagua.app.servicio.ObservadorRutaAbiertaGps
import pe.enrutatuagua.app.servicio.PrecacheRutaObservador
import pe.enrutatuagua.app.ui.Navegacion
import pe.enrutatuagua.designsystem.theme.TemaAgua
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var observadorRutaAbiertaGps: ObservadorRutaAbiertaGps
    @Inject lateinit var precacheRutaObservador: PrecacheRutaObservador

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // ANDROID-PLAN-04: se arranca acá (no en Application.onCreate())
        // porque la app es single-Activity -- MainActivity se crea una sola
        // vez por lanzamiento real y sobrevive la navegación entre pantallas
        // Compose, pero a diferencia de Application no se instancia en los
        // tests Robolectric que usan createComposeRule() (una Activity de
        // prueba genérica, no MainActivity) -- así los tests de pantallas
        // sueltas (ParadaScreenTest, PresidenteNavTest, etc.) no disparan un
        // observador de fondo tocando el Room real por accidente.
        // Hallazgo real (crash en Huawei P Smart 2019 sin ruta al backend):
        // un SocketTimeoutException acá no tenía quién lo atrape -- a
        // diferencia de RutaViewModel (que sólo protege SU PROPIA
        // colección), estos dos observadores corren en un scope aparte;
        // sin manejador, cualquier corte de red tumbaba la app entera
        // desde un hilo de fondo.
        val manejadorErrores = CoroutineExceptionHandler { _, error ->
            Log.w("MainActivity", "Observador de fondo falló, se ignora (best-effort)", error)
        }
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default + manejadorErrores)
        observadorRutaAbiertaGps.iniciar(alcance)
        precacheRutaObservador.iniciar(alcance)
        setContent {
            TemaAgua {
                Navegacion()
            }
        }
    }
}
