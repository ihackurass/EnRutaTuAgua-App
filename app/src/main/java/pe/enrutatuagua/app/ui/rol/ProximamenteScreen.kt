package pe.enrutatuagua.app.ui.rol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.enrutatuagua.designsystem.component.BotonSecundario
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.domain.modelo.Rol

@Composable
fun ProximamenteScreen(rol: Rol, onVolver: () -> Unit) {
    val paleta = LocalPaletaAgua.current
    val plan = when (rol) {
        Rol.VECINO -> "ANDROID-PLAN-05 (mapa en vivo)"
        Rol.PRESIDENTE -> "ANDROID-PLAN-06 (pedidos y validación)"
        else -> "un plan futuro"
    }
    Column(
        Modifier.fillMaxSize().background(paleta.fondo).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.LocalShipping, contentDescription = null,
            tint = paleta.texto2, modifier = Modifier.size(64.dp))
        Text("Hola, ${rol.etiqueta}", style = EstilosAgua.TituloParada,
            color = paleta.texto, textAlign = TextAlign.Center)
        Text("Tu módulo llega con $plan.", style = EstilosAgua.Cuerpo,
            color = paleta.texto2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        BotonSecundario("Cambiar de rol", onClick = onVolver, modifier = Modifier.fillMaxWidth())
    }
}
