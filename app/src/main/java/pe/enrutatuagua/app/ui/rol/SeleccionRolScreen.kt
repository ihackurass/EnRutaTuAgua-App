package pe.enrutatuagua.app.ui.rol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.RotuloSeccion
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.domain.modelo.Rol

@Composable
fun SeleccionRolScreen(onRolElegido: (Rol) -> Unit) {
    val paleta = LocalPaletaAgua.current
    Column(
        Modifier.fillMaxSize().background(paleta.fondo).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.WaterDrop, contentDescription = null,
            tint = paleta.accion, modifier = Modifier.size(44.dp))
        Text(
            "EnRutaTuAgua",
            style = EstilosAgua.TituloParada.copy(fontSize = 32.sp, lineHeight = 34.sp),
            color = paleta.texto,
        )
        RotuloSeccion("Selector provisional (solo pruebas)")
        Spacer(Modifier.height(10.dp))
        Rol.entries.forEach { rol ->
            BotonPrimario(rol.etiqueta, onClick = { onRolElegido(rol) })
        }
    }
}
