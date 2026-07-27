package pe.enrutatuagua.app.ubicacion

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Estado observable del permiso de ubicación fina. */
@Stable
class EstadoPermisoUbicacion internal constructor(inicial: Boolean) {
    var concedido by mutableStateOf(inicial)
        internal set

    internal var lanzador: () -> Unit = {}

    fun solicitar() = lanzador()
}

/**
 * Permiso ACCESS_FINE_LOCATION con el contrato ActivityResult (idiomático en
 * una app single-Activity Compose; el PermissionsManager de MapLibre exige
 * cablear onRequestPermissionsResult en la Activity).
 */
@Composable
fun rememberPermisoUbicacion(): EstadoPermisoUbicacion {
    val contexto = LocalContext.current
    val estado = remember {
        EstadoPermisoUbicacion(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        estado.concedido = it
    }
    estado.lanzador = { lanzador.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
    return estado
}
