package pe.enrutatuagua.app.ui.rol

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.domain.modelo.Rol

@Composable
fun ProximamenteScreen(rol: Rol, onVolver: () -> Unit) {
    val plan = when (rol) {
        Rol.VECINO -> "ANDROID-PLAN-05 (mapa en vivo)"
        Rol.PRESIDENTE -> "ANDROID-PLAN-06 (pedidos y validación)"
        else -> "un plan futuro"
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Hola, ${rol.etiqueta}", style = MaterialTheme.typography.headlineSmall)
        Text("Tu módulo llega con $plan.")
        TextButton(onClick = onVolver) { Text("Cambiar de rol") }
    }
}
