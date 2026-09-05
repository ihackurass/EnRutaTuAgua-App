package pe.enrutatuagua.app.ui.presidente

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.app.R
import pe.enrutatuagua.designsystem.component.BotonAtras
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.RotuloSeccion
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento

@Composable
fun ValidarPuntosScreen(onVolver: () -> Unit = {}, mostrarAtras: Boolean = true, vm: ValidarPuntosViewModel = hiltViewModel()) {
    val puntos by vm.puntos.collectAsState()
    val error by vm.error.collectAsState()
    val p = LocalPaletaAgua.current

    Column(
        Modifier.fillMaxSize().background(p.fondo).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        if (mostrarAtras) {
            Spacer(Modifier.height(8.dp))
            BotonAtras(onClick = onVolver)
            Spacer(Modifier.height(24.dp))
        } else Spacer(Modifier.height(8.dp))
        RotuloSeccion("Validar puntos de tu zona")
        Spacer(Modifier.height(6.dp))
        Text(
            "Puntos de abastecimiento",
            style = EstilosAgua.TituloParada,
            color = p.texto,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Confirma que cada lugar sea adecuado antes de incluirlo en las rutas.",
            style = EstilosAgua.Cuerpo,
            color = p.texto2,
        )
        Spacer(Modifier.height(18.dp))

        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = p.tinta,
        ) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ill_presidente_puntos),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("PENDIENTES DE REVISIÓN", style = EstilosAgua.Rotulo, color = p.sobreTinta)
                    Text(
                        "${puntos.size} ${if (puntos.size == 1) "punto" else "puntos"}",
                        style = EstilosAgua.DatoResumen,
                        color = p.sobreTinta,
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        error?.let {
            TarjetaError(it)
            Spacer(Modifier.height(12.dp))
        }

        if (puntos.isEmpty()) {
            EstadoSinPuntos(Modifier.weight(1f))
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 18.dp),
            ) {
                items(puntos, key = { it.id }) { punto ->
                    PuntoPorValidar(punto, onValidar = { vm.validar(punto.id) })
                }
            }
        }
    }
}

@Composable
private fun EstadoSinPuntos(modifier: Modifier = Modifier) {
    val p = LocalPaletaAgua.current
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_presidente_validado),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text("Todo está revisado", style = EstilosAgua.ItemTitulo, color = p.texto)
            Text("No hay puntos pendientes de aprobación", style = EstilosAgua.Cuerpo, color = p.texto2)
        }
    }
}

@Composable
private fun PuntoPorValidar(punto: PuntoAbastecimiento, onValidar: () -> Unit) {
    val p = LocalPaletaAgua.current
    val forma = RoundedCornerShape(12.dp)
    Surface(
        Modifier.fillMaxWidth().border(1.dp, p.linea, forma),
        shape = forma,
        color = p.hoja,
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(8.dp), color = p.superficie2) {
                    Text(
                        "#${punto.id}",
                        style = EstilosAgua.ItemTitulo,
                        color = p.texto2,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(punto.nombre, style = EstilosAgua.ItemTitulo, color = p.texto)
                    Text(
                        "${formatearLitros(punto.capacidadLitros)} L de capacidad",
                        style = EstilosAgua.Cuerpo,
                        color = p.texto2,
                    )
                }
            }
            Spacer(Modifier.height(15.dp))
            BotonPrimario(
                texto = "Aprobar punto",
                onClick = onValidar,
                iconoPainter = painterResource(R.drawable.ic_presidente_validado),
            )
        }
    }
}
