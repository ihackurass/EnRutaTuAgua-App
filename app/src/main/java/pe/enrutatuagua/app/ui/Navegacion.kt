package pe.enrutatuagua.app.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pe.enrutatuagua.app.ui.rol.ProximamenteScreen
import pe.enrutatuagua.app.ui.rol.SeleccionRolScreen
import pe.enrutatuagua.app.ui.rol.SesionViewModel
import pe.enrutatuagua.domain.modelo.Rol

@Composable
fun Navegacion() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "rol") {
        composable("rol") {
            val vm: SesionViewModel = hiltViewModel()
            SeleccionRolScreen(onRolElegido = { rol ->
                vm.seleccionarRol(rol)
                when (rol) {
                    Rol.AGUATERO -> nav.navigate("ruta")
                    else -> nav.navigate("proximamente/${rol.name}")
                }
            })
        }
        composable("proximamente/{rol}") { entrada ->
            val rol = entrada.arguments?.getString("rol")
                ?.let { n -> Rol.entries.firstOrNull { it.name == n } } ?: Rol.VECINO
            ProximamenteScreen(rol = rol, onVolver = { nav.popBackStack() })
        }
        // "ruta" y "parada/{paradaId}" se agregan en T5 y T7
    }
}
