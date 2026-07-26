package pe.enrutatuagua.app.ui.presidente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.designsystem.component.BotonAtras
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.CifraLitros
import pe.enrutatuagua.designsystem.component.EstadoFicha
import pe.enrutatuagua.designsystem.component.FichaParada
import pe.enrutatuagua.designsystem.component.RotuloSeccion
import pe.enrutatuagua.designsystem.component.TecladoLitros
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua

/**
 * Nuevo pedido (RP-01). Selector de punto validado + cifra/teclado de litros,
 * reutilizados tal cual del flujo de entrega del aguatero (ver `ParadaScreen`):
 * es exactamente el mismo problema de UI, "cuántos litros".
 */
@Composable
fun NuevoPedidoScreen(onHecho: () -> Unit, vm: NuevoPedidoViewModel = hiltViewModel()) {
    val puntos by vm.puntos.collectAsState()
    val hecho by vm.hecho.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(hecho) { if (hecho) onHecho() }

    var puntoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var litros by rememberSaveable { mutableStateOf("") }

    val paleta = LocalPaletaAgua.current
    val valor = litros.toIntOrNull() ?: 0
    val invalido = puntoId == null || valor <= 0

    Column(
        Modifier
            .fillMaxSize()
            .background(paleta.hoja)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BotonAtras(onClick = onHecho)
        Spacer(Modifier.height(24.dp))

        RotuloSeccion("Nuevo pedido")
        Spacer(Modifier.height(10.dp))
        Text("Elige el punto de tu zona", style = EstilosAgua.ItemTitulo, color = paleta.texto)
        Spacer(Modifier.height(6.dp))

        // Lista simple (no lazy, no scroll propio): igual que el resto de la
        // pantalla, todo el contenido vive en el mismo Column no-scrolleable
        // que `ParadaScreen` — la cifra y el teclado quedan siempre al
        // alcance del pulgar, sin depender de que el usuario haga scroll.
        puntos.forEach { punto ->
            FichaParada(
                numero = punto.id.toInt(),
                titulo = punto.nombre,
                subtitulo = "${formatearLitros(punto.capacidadLitros)} L de capacidad",
                textoDerecha = if (puntoId == punto.id) "Elegido" else "",
                estado = if (puntoId == punto.id) EstadoFicha.Proxima else EstadoFicha.Pendiente,
                onClick = { puntoId = punto.id },
            )
        }
        Spacer(Modifier.height(16.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CifraLitros(valor)
        }
        Spacer(Modifier.height(16.dp))
        TecladoLitros(valor = litros, onCambio = { litros = it })

        error?.let {
            Spacer(Modifier.height(12.dp))
            TarjetaError(it)
        }
        Spacer(Modifier.height(12.dp))
        BotonPrimario(
            texto = "Pedir agua",
            habilitado = !invalido,
            onClick = { puntoId?.let { id -> vm.crear(id, valor) } },
        )
    }
}
