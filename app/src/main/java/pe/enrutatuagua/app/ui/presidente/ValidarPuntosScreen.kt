package pe.enrutatuagua.app.ui.presidente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.designsystem.component.BotonAtras
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.EstadoFicha
import pe.enrutatuagua.designsystem.component.FichaParada
import pe.enrutatuagua.designsystem.component.RotuloSeccion
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento

/**
 * Validar puntos propuestos de la zona (RP-02, alcance estricto: solo
 * aprueba — ver "Fuera de alcance" del plan 06 para rechazar/suspender/
 * proponer).
 */
@Composable
fun ValidarPuntosScreen(onVolver: () -> Unit, vm: ValidarPuntosViewModel = hiltViewModel()) {
    val puntos by vm.puntos.collectAsState()
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
        Spacer(Modifier.height(8.dp))
        BotonAtras(onClick = onVolver)
        Spacer(Modifier.height(24.dp))

        RotuloSeccion("Validar puntos de tu zona")
        Spacer(Modifier.height(6.dp))
        Text(
            "Solo los puntos aprobados pueden recibir cisterna",
            style = EstilosAgua.Cuerpo, color = paleta.texto2,
        )
        Spacer(Modifier.height(14.dp))

        if (puntos.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "No hay puntos pendientes de aprobación",
                    style = EstilosAgua.TituloParada, color = paleta.texto2,
                )
            }
        } else {
            error?.let {
                TarjetaError(it)
                Spacer(Modifier.height(12.dp))
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(puntos, key = { it.id }) { punto ->
                    FilaPunto(punto, onValidar = { vm.validar(punto.id) })
                }
            }
        }
    }
}

@Composable
private fun FilaPunto(punto: PuntoAbastecimiento, onValidar: () -> Unit) {
    Column {
        FichaParada(
            numero = punto.id.toInt(),
            titulo = punto.nombre,
            subtitulo = "${formatearLitros(punto.capacidadLitros)} L de capacidad",
            textoDerecha = punto.estado.etiqueta,
            estado = EstadoFicha.Proxima,
        )
        Spacer(Modifier.height(8.dp))
        BotonPrimario("Validar", onClick = onValidar)
        Spacer(Modifier.height(8.dp))
    }
}
