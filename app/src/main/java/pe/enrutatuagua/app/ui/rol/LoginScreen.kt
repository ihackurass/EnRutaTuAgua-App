package pe.enrutatuagua.app.ui.rol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

@Composable
fun LoginScreen(procesando: Boolean, error: String?, onIngresar: (String, String) -> Unit) {
    var dni by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    val paleta = LocalPaletaAgua.current
    Column(
        Modifier.fillMaxSize().background(paleta.fondo).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("EnRutaTuAgua", style = EstilosAgua.TituloParada, color = paleta.texto)
        Text("Ingresa con tu cuenta", style = EstilosAgua.Cuerpo, color = paleta.texto2)
        OutlinedTextField(
            value = dni, onValueChange = { dni = it.filter(Char::isDigit).take(12) },
            label = { Text("DNI") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            value = clave, onValueChange = { clave = it }, label = { Text("Contraseña") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        error?.let { Text(it, color = paleta.error, style = EstilosAgua.Cuerpo) }
        BotonPrimario(if (procesando) "Ingresando…" else "Ingresar", onClick = {
            if (!procesando) onIngresar(dni, clave)
        })
    }
}
