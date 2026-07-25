package pe.enrutatuagua.app.ui.rol

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.domain.modelo.Rol

@Composable
fun SeleccionRolScreen(onRolElegido: (Rol) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("EnRutaTuAgua", style = MaterialTheme.typography.headlineLarge)
        Text("Selector provisional (solo pruebas)", style = MaterialTheme.typography.labelMedium)
        Rol.entries.forEach { rol ->
            Button(onClick = { onRolElegido(rol) }, modifier = Modifier.fillMaxWidth()) {
                Text(rol.etiqueta)
            }
        }
    }
}
