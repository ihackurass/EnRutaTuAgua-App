package pe.enrutatuagua.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.enrutatuagua.app.ui.mapa.MapaDiagnosticoScreen
import pe.enrutatuagua.app.ui.parada.ParadaScreen
import pe.enrutatuagua.app.ui.presidente.PresidenteNav
import pe.enrutatuagua.app.ui.rol.ProximamenteScreen
import pe.enrutatuagua.app.ui.rol.SeleccionRolScreen
import pe.enrutatuagua.app.ui.rol.SesionViewModel
import pe.enrutatuagua.app.ui.ruta.RutaScreen
import pe.enrutatuagua.app.ui.vecino.MapaVivoScreen
import pe.enrutatuagua.designsystem.tokens.MovimientoAgua
import pe.enrutatuagua.domain.modelo.Rol

@Composable
fun Navegacion() {
    val nav = rememberNavController()
    NavHost(
        nav,
        startDestination = "rol",
        enterTransition = {
            slideInHorizontally(tween(MovimientoAgua.Hoja, easing = MovimientoAgua.EasingSalida)) { it / 4 } +
                fadeIn(tween(MovimientoAgua.Hoja))
        },
        exitTransition = { fadeOut(tween(MovimientoAgua.Perilla)) },
        popEnterTransition = { fadeIn(tween(MovimientoAgua.Hoja)) },
        popExitTransition = {
            slideOutHorizontally(tween(MovimientoAgua.Hoja, easing = MovimientoAgua.EasingSalida)) { it / 4 } +
                fadeOut(tween(MovimientoAgua.Hoja))
        },
    ) {
        composable("rol") {
            val vm: SesionViewModel = hiltViewModel()
            SeleccionRolScreen(onRolElegido = { rol ->
                vm.seleccionarRol(rol)
                when (rol) {
                    Rol.AGUATERO -> nav.navigate("ruta")
                    Rol.VECINO -> nav.navigate("vecino")
                    Rol.PRESIDENTE -> nav.navigate("presidente")
                    Rol.ADMIN -> nav.navigate("proximamente")
                }
            }, onDiagnosticoMapaClick = { nav.navigate("mapa-diagnostico") })
        }
        composable("mapa-diagnostico") { MapaDiagnosticoScreen() }
        composable("proximamente") {
            ProximamenteScreen(onVolver = { nav.popBackStack() })
        }
        composable("vecino") { MapaVivoScreen() }
        composable("presidente") { PresidenteNav() }
        composable("ruta") { entrada ->
            val aviso by entrada.savedStateHandle
                .getStateFlow<String?>("aviso", null)
                .collectAsState()
            RutaScreen(
                onParadaClick = { id -> nav.navigate("parada/$id") },
                onOmitirClick = { id -> nav.navigate("parada/$id?omitir=true") },
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
