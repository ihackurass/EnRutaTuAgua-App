package pe.enrutatuagua.app.ubicacion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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

/** Estado observable de un permiso simple (concedido/no) con una acción para pedirlo. */
@Stable
class EstadoPermisoSimple internal constructor(inicial: Boolean) {
    var concedido by mutableStateOf(inicial)
        internal set

    internal var accion: () -> Unit = {}

    fun solicitar() = accion()
}

/**
 * Permiso de ubicación en segundo plano (RA-07: "con explicación clara").
 * API < 29: ACCESS_FINE_LOCATION ya cubre background, nada que pedir. API
 * 29: diálogo runtime real. API 30+: el sistema ya no ofrece "Allow all the
 * time" inline -- técnicamente requeriría llevar al usuario a los ajustes
 * de la app, pero eso saca a el aguatero de la app (percibido como "se
 * cierra"), así que por pedido del usuario se saca ese salto: en API 30+
 * simplemente no se puede pedir programáticamente, se deja como está.
 */
@Composable
fun rememberPermisoUbicacionSegundoPlano(): EstadoPermisoSimple {
    val contexto = LocalContext.current
    val estado = remember {
        EstadoPermisoSimple(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED,
        )
    }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        estado.concedido = it
    }
    estado.accion = {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> estado.concedido = true
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ->
                lanzador.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            else -> Unit
        }
    }
    return estado
}

/** POST_NOTIFICATIONS (API 33+) -- exigido para que la notificación persistente del tracking se muestre. */
@Composable
fun rememberPermisoNotificaciones(): EstadoPermisoSimple {
    val contexto = LocalContext.current
    val estado = remember {
        EstadoPermisoSimple(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED,
        )
    }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        estado.concedido = it
    }
    estado.accion = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            estado.concedido = true
        }
    }
    return estado
}
