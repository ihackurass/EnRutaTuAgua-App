package pe.enrutatuagua.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
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
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        observadorRutaAbiertaGps.iniciar(alcance)
        precacheRutaObservador.iniciar(alcance)
        setContent {
            TemaAgua {
                Navegacion()
            }
        }
    }
}
