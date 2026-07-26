package pe.enrutatuagua.app.ui.presidente

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Un solo punto de entrada ("presidente") en el NavHost principal (se cablea
// en la fase de integración) — todo lo demás es interno a este composable,
// por eso el plan no toca app/ui/Navegacion.kt.
@Composable
fun PresidenteNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "mis-pedidos") {
        composable("mis-pedidos") {
            MisPedidosScreen(
                onNuevoPedido = { nav.navigate("nuevo-pedido") },
                onValidarPuntos = { nav.navigate("validar-puntos") },
            )
        }
        composable("nuevo-pedido") { NuevoPedidoScreen(onHecho = { nav.popBackStack() }) }
        composable("validar-puntos") { ValidarPuntosScreen(onVolver = { nav.popBackStack() }) }
    }
}
