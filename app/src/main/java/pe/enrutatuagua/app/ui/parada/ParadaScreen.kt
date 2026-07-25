package pe.enrutatuagua.app.ui.parada

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.domain.modelo.MotivoOmision

@Composable
fun ParadaScreen(onHecho: () -> Unit, vm: ParadaViewModel = hiltViewModel()) {
    val hecho by vm.hecho.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(hecho) { if (hecho) onHecho() }

    var litros by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf<MotivoOmision?>(null) }
    var comentario by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Registrar entrega", style = MaterialTheme.typography.headlineSmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(
            value = litros,
            onValueChange = { nuevo -> litros = nuevo.filter(Char::isDigit) },
            label = { Text("Litros entregados") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Button(
            enabled = litros.toIntOrNull()?.let { it > 0 } == true,
            onClick = { vm.confirmarEntrega(litros.toInt()) },
        ) { Text("Marcar entregado") }

        HorizontalDivider()
        Text("¿No se pudo atender? Elige el motivo:", style = MaterialTheme.typography.titleSmall)
        MotivoOmision.entries.forEach { m ->
            FilterChip(selected = motivo == m, onClick = { motivo = m }, label = { Text(m.etiqueta) })
        }
        if (motivo == MotivoOmision.OTRO) {
            OutlinedTextField(value = comentario, onValueChange = { comentario = it },
                label = { Text("Explica el motivo (obligatorio)") })
        }
        OutlinedButton(
            enabled = motivo != null,
            onClick = { vm.omitir(motivo!!, comentario.takeIf { it.isNotBlank() }) },
        ) { Text("Omitir parada") }
    }
}
