package pe.enrutatuagua.app.ui.rol

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronRight
import pe.enrutatuagua.app.R
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.domain.modelo.Rol

@Composable
fun SeleccionRolScreen(
    onRolElegido: (Rol) -> Unit,
    roles: Set<Rol> = Rol.entries.toSet(),
) {
    val paleta = LocalPaletaAgua.current
    val rolesDisponibles = Rol.entries.filter { it in roles }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(paleta.fondo),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ill_login_marca),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Elige cómo deseas ingresar",
                    style = EstilosAgua.TituloParada.copy(fontSize = 26.sp, lineHeight = 30.sp),
                    color = paleta.texto,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "Puedes cambiar de rol más adelante",
                    style = EstilosAgua.Cuerpo,
                    color = paleta.texto2,
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        items(rolesDisponibles, key = { it.name }) { rol ->
            TarjetaRol(rol = rol, onClick = { onRolElegido(rol) })
        }

    }
}

@Composable
private fun TarjetaRol(rol: Rol, onClick: () -> Unit) {
    val paleta = LocalPaletaAgua.current
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp)
            .testTag("rol-${rol.name.lowercase()}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = paleta.hoja),
        border = BorderStroke(1.dp, paleta.linea),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(rol.ilustracion),
                    contentDescription = null,
                    modifier = Modifier.size(78.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rol.etiqueta, style = EstilosAgua.BotonPrimario, color = paleta.texto)
                Spacer(Modifier.height(3.dp))
                Text(rol.descripcion, style = EstilosAgua.Cuerpo, color = paleta.texto2)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = TablerIcons.ChevronRight,
                contentDescription = null,
                tint = paleta.accion,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private val Rol.descripcion: String
    get() = when (this) {
        Rol.AGUATERO -> "Gestiona tu ruta, carga y entregas"
        Rol.VECINO -> "Sigue la cisterna en tiempo real"
        Rol.PRESIDENTE -> "Solicita agua y administra tus puntos"
        Rol.ADMIN -> "Configura y supervisa la plataforma"
        Rol.OPERADOR -> "Planifica rutas, vehículos y jornadas"
    }

private val Rol.ilustracion: Int
    @DrawableRes get() = when (this) {
        Rol.AGUATERO -> R.drawable.ill_rol_aguatero
        Rol.VECINO -> R.drawable.ill_rol_vecino
        Rol.PRESIDENTE -> R.drawable.ill_rol_presidente
        Rol.ADMIN -> R.drawable.ill_rol_administrador
        Rol.OPERADOR -> R.drawable.ill_rol_operador
    }
