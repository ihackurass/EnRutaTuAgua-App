package pe.enrutatuagua.app.ui.rol

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.app.R
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

@Composable
fun LoginScreen(
    procesando: Boolean,
    error: String?,
    onIngresar: (String, String) -> Unit,
    mostrarAccesosRapidos: Boolean = false,
) {
    var dni by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var mostrarClave by rememberSaveable { mutableStateOf(false) }
    val paleta = LocalPaletaAgua.current
    Column(
        Modifier
            .fillMaxSize()
            .background(paleta.fondo)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ill_login_marca),
            contentDescription = null,
            modifier = Modifier.size(104.dp),
        )
        Text("EnRutaTuAgua", style = EstilosAgua.TituloParada, color = paleta.texto)
        Text("Ingresa con tu cuenta", style = EstilosAgua.Cuerpo, color = paleta.texto2)
        OutlinedTextField(
            value = dni, onValueChange = { dni = it.filter(Char::isDigit).take(12) },
            label = { Text("DNI") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = {
                Image(
                    painter = painterResource(R.drawable.ic_login_dni),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
            },
        )
        OutlinedTextField(
            value = clave, onValueChange = { clave = it }, label = { Text("Contraseña") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (mostrarClave) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = {
                Image(
                    painter = painterResource(R.drawable.ic_login_clave),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { mostrarClave = !mostrarClave },
                    modifier = Modifier.testTag("alternar-visibilidad-clave"),
                ) {
                    Image(
                        painter = painterResource(
                            if (mostrarClave) R.drawable.ic_login_ocultar_clave
                            else R.drawable.ic_login_mostrar_clave,
                        ),
                        contentDescription = if (mostrarClave) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        },
                        modifier = Modifier.size(26.dp),
                    )
                }
            },
        )
        error?.let { Text(it, color = paleta.error, style = EstilosAgua.Cuerpo) }
        BotonPrimario(
            texto = if (procesando) "Ingresando…" else "Ingresar",
            onClick = { if (!procesando) onIngresar(dni, clave) },
            habilitado = !procesando,
            iconoPainter = painterResource(R.drawable.ic_login_ingresar),
        )
        if (mostrarAccesosRapidos) {
            Text("Acceso rápido de prueba", style = EstilosAgua.Cuerpo, color = paleta.texto2)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccesoRapido("Presidente", procesando) { onIngresar("10000001", "presidente1234") }
                    AccesoRapido("Aguatero", procesando) { onIngresar("10000003", "aguatero1234") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccesoRapido("Vecino", procesando) { onIngresar("10000002", "vecino1234") }
                    AccesoRapido("Operador", procesando) { onIngresar("10000005", "operador1234") }
                }
            }
        }
    }
}

@Composable
private fun AccesoRapido(texto: String, procesando: Boolean, onClick: () -> Unit) {
    AssistChip(
        modifier = Modifier.testTag("acceso-${texto.lowercase()}"),
        onClick = onClick,
        enabled = !procesando,
        label = { Text(texto, style = EstilosAgua.Cuerpo) },
    )
}
