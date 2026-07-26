package pe.enrutatuagua.app.ui.presidente

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.BotonSecundario
import pe.enrutatuagua.designsystem.component.EstadoFicha
import pe.enrutatuagua.designsystem.component.FichaParada
import pe.enrutatuagua.designsystem.component.RotuloSeccion
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.domain.modelo.EstadoPedido
import pe.enrutatuagua.domain.modelo.Pedido

/**
 * Mis pedidos (RP-03 ver estado, RP-04 cancelar). Reutiliza `FichaParada` del
 * design system tal cual (API genérica: numero+titulo+subtitulo+textoDerecha+
 * estado, no está acoplada a paradas del aguatero).
 */
@Composable
fun MisPedidosScreen(
    onNuevoPedido: () -> Unit,
    onValidarPuntos: () -> Unit,
    vm: MisPedidosViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsState()
    val error by vm.error.collectAsState()
    val paleta = LocalPaletaAgua.current

    Column(
        Modifier
            .fillMaxSize()
            .background(paleta.hoja)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RotuloSeccion("Mis pedidos", modifier = Modifier.weight(1f))
            Text(
                "Validar puntos",
                style = EstilosAgua.ItemTitulo,
                color = paleta.accion,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onValidarPuntos)
                    .padding(horizontal = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))

        when {
            estado.cargando -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = paleta.accion)
            }
            estado.pedidos.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Todavía no tienes pedidos", style = EstilosAgua.TituloParada, color = paleta.texto2)
            }
            else -> LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(estado.pedidos, key = { it.id }) { pedido ->
                    FilaPedido(pedido, onCancelar = { vm.cancelar(pedido.id) })
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(10.dp))
            TarjetaError(it)
        }
        Spacer(Modifier.height(12.dp))
        BotonPrimario("Nuevo pedido", onClick = onNuevoPedido)
    }
}

@Composable
private fun FilaPedido(pedido: Pedido, onCancelar: () -> Unit) {
    Column {
        FichaParada(
            numero = pedido.id.toInt(),
            titulo = "Pedido #${pedido.id}",
            subtitulo = "${formatearLitros(pedido.items.sumOf { it.litros })} L",
            textoDerecha = pedido.estado.etiqueta,
            estado = when (pedido.estado) {
                EstadoPedido.ATENDIDO -> EstadoFicha.Entregada
                EstadoPedido.CANCELADO, EstadoPedido.RETENIDO -> EstadoFicha.Omitida
                else -> EstadoFicha.Pendiente
            },
        )
        if (pedido.estado == EstadoPedido.PENDIENTE || pedido.estado == EstadoPedido.RETENIDO) {
            Spacer(Modifier.height(8.dp))
            BotonSecundario("Cancelar", onClick = onCancelar)
            Spacer(Modifier.height(8.dp))
        }
    }
}
