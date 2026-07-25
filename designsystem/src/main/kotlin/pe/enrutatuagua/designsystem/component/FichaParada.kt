package pe.enrutatuagua.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.X
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua

enum class EstadoFicha { Pendiente, Proxima, Entregada, Omitida }

@Composable
fun FichaParada(
    numero: Int,
    titulo: String,
    subtitulo: String,
    textoDerecha: String,
    estado: EstadoFicha,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val paleta = LocalPaletaAgua.current
    Column(modifier.alpha(if (onClick == null && estado != EstadoFicha.Proxima) 0.62f else 1f)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = DimensionesAgua.AltoTactilMinimo)
                .let { m -> onClick?.let { m.clickable(onClick = it) } ?: m }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(DimensionesAgua.Badge)
                    .clip(RoundedCornerShape(RadiosAgua.Badge))
                    .background(
                        when (estado) {
                            EstadoFicha.Proxima -> paleta.accion
                            EstadoFicha.Entregada -> paleta.exito.copy(alpha = 0.16f)
                            EstadoFicha.Omitida -> paleta.alerta.copy(alpha = 0.16f)
                            EstadoFicha.Pendiente -> paleta.tenue
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when (estado) {
                    EstadoFicha.Entregada ->
                        Icon(TablerIcons.Check, null, tint = paleta.exito, modifier = Modifier.size(16.dp))
                    EstadoFicha.Omitida ->
                        Icon(TablerIcons.X, null, tint = paleta.alerta, modifier = Modifier.size(16.dp))
                    else -> Text(
                        "$numero",
                        style = EstilosAgua.ItemSub.copy(fontWeight = FontWeight.Bold),
                        color = if (estado == EstadoFicha.Proxima) Color.White else paleta.texto2,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(titulo, style = EstilosAgua.ItemTitulo, color = paleta.texto,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitulo, style = EstilosAgua.ItemSub, color = paleta.texto2)
            }
            Text(
                textoDerecha,
                style = if (estado == EstadoFicha.Omitida) EstilosAgua.ItemSub else EstilosAgua.Preset,
                color = when (estado) {
                    EstadoFicha.Entregada -> paleta.exito
                    EstadoFicha.Omitida -> paleta.alerta
                    else -> paleta.texto
                },
            )
        }
        HorizontalDivider(color = paleta.linea)
    }
}
