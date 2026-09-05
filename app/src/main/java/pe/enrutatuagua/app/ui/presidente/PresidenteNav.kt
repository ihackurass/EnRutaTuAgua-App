package pe.enrutatuagua.app.ui.presidente

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import compose.icons.TablerIcons
import compose.icons.tablericons.ClipboardList
import compose.icons.tablericons.Droplet
import compose.icons.tablericons.MapPin
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

private data class SeccionPresidente(val ruta:String,val etiqueta:String,val icono:ImageVector)
private val secciones=listOf(
    SeccionPresidente("pedidos","Pedidos",TablerIcons.ClipboardList),
    SeccionPresidente("recepciones","Recepciones",TablerIcons.Droplet),
    SeccionPresidente("zona","Zona",TablerIcons.MapPin),
)

@Composable
fun PresidenteNav(recepcionesVm:RecepcionesViewModel= hiltViewModel()) {
    val nav=rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val ruta=entrada?.destination?.route
    val recepciones by recepcionesVm.estado.collectAsState()
    val p=LocalPaletaAgua.current
    val mostrarBarra=ruta in secciones.map{it.ruta}
    Scaffold(bottomBar={if(mostrarBarra) NavigationBar(containerColor=p.hoja){secciones.forEach{seccion->
        NavigationBarItem(
            selected=ruta==seccion.ruta,
            onClick={nav.navigate(seccion.ruta){popUpTo(nav.graph.findStartDestination().id){saveState=true};launchSingleTop=true;restoreState=true}},
            icon={
                if(seccion.ruta=="recepciones"&&recepciones.totalPendientes>0)
                    BadgedBox(badge={Badge{Text(recepciones.totalPendientes.coerceAtMost(99).toString())}}){Icon(seccion.icono,null)}
                else Icon(seccion.icono,null)
            },
            label={Text(seccion.etiqueta)},
        )
    }}}){padding->
        NavHost(nav,startDestination="pedidos",modifier=Modifier.padding(padding)){
            composable("pedidos"){MisPedidosScreen(onNuevoPedido={nav.navigate("nuevo-pedido")})}
            composable("recepciones"){RecepcionesScreen(onHistorial={nav.navigate("historial-recepciones")},vm=recepcionesVm)}
            composable("zona"){ValidarPuntosScreen(mostrarAtras=false)}
            composable("nuevo-pedido"){NuevoPedidoScreen(onHecho={nav.popBackStack()})}
            composable("historial-recepciones"){HistorialRecepcionesScreen(onVolver={nav.popBackStack()},vm=recepcionesVm)}
        }
    }
}
