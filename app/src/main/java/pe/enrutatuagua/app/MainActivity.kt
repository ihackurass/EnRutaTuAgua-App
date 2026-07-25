package pe.enrutatuagua.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import pe.enrutatuagua.app.ui.Navegacion
import pe.enrutatuagua.designsystem.theme.TemaAgua

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TemaAgua {
                Navegacion()
            }
        }
    }
}
