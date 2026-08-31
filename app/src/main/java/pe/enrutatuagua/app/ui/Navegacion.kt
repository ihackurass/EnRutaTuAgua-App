package pe.enrutatuagua.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import pe.enrutatuagua.app.ui.operador.OperacionScreen
import pe.enrutatuagua.app.ui.ruta.RutaAsignadaGate
import pe.enrutatuagua.app.ui.tracking.PruebaTrackingScreen
import pe.enrutatuagua.app.BuildConfig
import pe.enrutatuagua.app.R
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.modelo.Sesion
import compose.icons.TablerIcons
import compose.icons.tablericons.Dots
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

@Composable
fun Navegacion(vm: SesionViewModel = hiltViewModel()) {
    val sesion by vm.sesion.collectAsState()
    val procesando by vm.procesando.collectAsState()
    val error by vm.error.collectAsState()
    val actual = sesion
    when {
        actual == null -> LoginScreen(
            procesando = procesando,
            error = error,
            onIngresar = vm::iniciar,
            mostrarAccesosRapidos = BuildConfig.DEBUG,
        )
        actual.rolActivo == null -> SelectorODiagnostico(actual, vm)
        else -> key(actual.usuarioId, actual.rolActivo) {
            AplicacionAutenticada(actual, vm::cerrar, vm::cambiarRol)
        }
    }
}

@Composable
private fun SelectorODiagnostico(sesion: Sesion, vm: SesionViewModel) {
    var diagnostico by rememberSaveable(sesion.usuarioId) { mutableStateOf(false) }
    if (diagnostico && BuildConfig.DEBUG) {
        PruebaTrackingScreen(onVolver = { diagnostico = false })
    } else {
        SeleccionRolScreen(
            roles = sesion.roles,
            onRolElegido = vm::seleccionarRol,
            mostrarDiagnosticos = BuildConfig.DEBUG,
            onPruebaTrackingClick = { diagnostico = true },
        )
    }
}

@Composable
private fun AplicacionAutenticada(sesion: Sesion, onCerrar: () -> Unit, onCambiarRol: () -> Unit) {
    val nav = rememberNavController()
    Box(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = sesion.rolActivo!!.name) {
            composable(Rol.AGUATERO.name) { RutaAsignadaGate(conRutaAbierta = { NavegacionAguatero() }) }
            composable(Rol.VECINO.name) { MapaVivoScreen() }
            composable(Rol.PRESIDENTE.name) { PresidenteNav() }
            composable(Rol.ADMIN.name) { ProximamenteScreen(onVolver = onCerrar) }
            composable(Rol.OPERADOR.name) { OperacionScreen() }
        }
        MenuSesion(sesion.roles.size > 1, onCambiarRol, onCerrar, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(6.dp))
    }
}

@Composable
private fun MenuSesion(multirrol: Boolean, onCambiarRol: () -> Unit, onCerrar: () -> Unit, modifier: Modifier = Modifier) {
    var abierto by remember { mutableStateOf(false) }
    val paleta = LocalPaletaAgua.current
    Box(modifier) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = paleta.accion,
                shadowElevation = 4.dp,
            ) {
                IconButton(onClick = { abierto = true }) {
                    Icon(
                        TablerIcons.Dots,
                        contentDescription = "Abrir menú de sesión",
                        tint = paleta.sobreTinta,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = paleta.hoja,
        ) {
            if (multirrol) DropdownMenuItem(
                text = { Text("Cambiar rol") },
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_sesion_cambiar_rol),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                },
                onClick = { abierto = false; onCambiarRol() },
            )
            DropdownMenuItem(
                text = { Text("Cerrar sesión") },
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_sesion_cerrar),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                },
                onClick = { abierto = false; onCerrar() },
            )
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
