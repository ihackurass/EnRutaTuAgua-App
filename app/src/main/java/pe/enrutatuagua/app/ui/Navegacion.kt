package pe.enrutatuagua.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.enrutatuagua.app.ui.parada.ParadaScreen
import pe.enrutatuagua.app.ui.presidente.PresidenteNav
import pe.enrutatuagua.app.ui.rol.LoginScreen
import pe.enrutatuagua.app.ui.rol.ProximamenteScreen
import pe.enrutatuagua.app.ui.rol.SeleccionRolScreen
import pe.enrutatuagua.app.ui.rol.SesionViewModel
import pe.enrutatuagua.app.ui.ruta.RutaScreen
import pe.enrutatuagua.app.ui.vecino.MapaVivoScreen
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.modelo.Sesion

@Composable
fun Navegacion(vm: SesionViewModel = hiltViewModel()) {
    val sesion by vm.sesion.collectAsState()
    val procesando by vm.procesando.collectAsState()
    val error by vm.error.collectAsState()
    val actual = sesion
    when {
        actual == null -> LoginScreen(procesando, error, vm::iniciar)
        actual.rolActivo == null -> SeleccionRolScreen(
            roles = actual.roles,
            onRolElegido = vm::seleccionarRol,
        )
        else -> AplicacionAutenticada(actual, vm::cerrar, vm::cambiarRol)
    }
}

@Composable
private fun AplicacionAutenticada(sesion: Sesion, onCerrar: () -> Unit, onCambiarRol: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        when (sesion.rolActivo) {
            Rol.AGUATERO -> NavegacionAguatero()
            Rol.VECINO -> MapaVivoScreen()
            Rol.PRESIDENTE -> PresidenteNav()
            Rol.ADMIN -> ProximamenteScreen(onVolver = onCerrar)
            null -> Unit
        }
        Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            if (sesion.roles.size > 1) TextButton(onClick = onCambiarRol) { Text("Cambiar rol") }
            TextButton(onClick = onCerrar) { Text("Salir") }
        }
    }
}

@Composable
private fun NavegacionAguatero() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "ruta") {
        composable("ruta") { entrada ->
            val aviso by entrada.savedStateHandle.getStateFlow<String?>("aviso", null).collectAsState()
            RutaScreen(
                onParadaClick = { nav.navigate("parada/$it") },
                onOmitirClick = { nav.navigate("parada/$it?omitir=true") },
                aviso = aviso,
                onAvisoMostrado = { entrada.savedStateHandle["aviso"] = null },
            )
        }
        composable(
            "parada/{paradaId}?omitir={omitir}",
            arguments = listOf(
                navArgument("paradaId") { type = NavType.LongType },
                navArgument("omitir") { type = NavType.BoolType; defaultValue = false },
            ),
        ) {
            ParadaScreen(onHecho = { mensaje ->
                mensaje?.let { nav.previousBackStackEntry?.savedStateHandle?.set("aviso", it) }
                nav.popBackStack()
            })
        }
    }
}
