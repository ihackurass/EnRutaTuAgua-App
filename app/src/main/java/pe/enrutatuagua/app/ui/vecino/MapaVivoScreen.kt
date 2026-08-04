package pe.enrutatuagua.app.ui.vecino

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import compose.icons.TablerIcons
import compose.icons.tablericons.WifiOff
import pe.enrutatuagua.designsystem.component.AvisoSuperior
import pe.enrutatuagua.designsystem.component.EstadoFicha
import pe.enrutatuagua.designsystem.component.FichaParada
import pe.enrutatuagua.designsystem.component.HojaArrastrable
import pe.enrutatuagua.designsystem.component.RotuloSeccion
import pe.enrutatuagua.designsystem.component.TarjetaDato
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.component.rememberEstadoAviso
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua
import pe.enrutatuagua.domain.modelo.PosicionCisterna
import pe.enrutatuagua.domain.modelo.PuntoAbastecimiento

/**
 * Mapa vivo del vecino (plan 05 T2): la cáscara honesta de RV-01 — última
 * posición conocida de la cisterna con su marca de tiempo, más los puntos de
 * abastecimiento de su zona en modo solo lectura (RV-02). El badge "sin
 * conexión en vivo" es SIEMPRE visible cuando hay posición: es el estado
 * normal de esta versión, no un error transitorio (ver §0 del plan).
 */
@Composable
fun MapaVivoScreen(
    vm: MapaVivoViewModel = hiltViewModel(),
    mapa: @Composable (List<PuntoAbastecimiento>, PosicionCisterna?) -> Unit =
        { puntos, posicion -> MapaZonaVecino(puntos, posicion, Modifier.fillMaxSize()) },
) {
    val estado by vm.estado.collectAsState()
    val mensaje by vm.mensaje.collectAsState()
    val paleta = LocalPaletaAgua.current
    val estadoAviso = rememberEstadoAviso()
    LaunchedEffect(mensaje) {
        mensaje?.let {
            estadoAviso.mostrar(it)
            vm.mensajeMostrado()
        }
    }
    // Auditoría rol Vecino: recalcula minutosSinSenal periódicamente sin
    // reemisión del backend -- vive acá (no en el ViewModel) a propósito:
    // un `while(true){delay()}` dentro de viewModelScope rompía tanto
    // Compose (waitForIdle nunca "asentaba") como los tests unitarios del
    // ViewModel (runTest nunca terminaba de drenar el scheduler
    // compartido). Acá, un test puede desactivarlo con mainClock.autoAdvance = false.
    LaunchedEffect(Unit) {
        while (true) {
            delay(INTERVALO_TICK_RELOJ_MS)
            vm.recalcularMinutosSinSenal()
        }
    }

    Box(Modifier.fillMaxSize().background(paleta.mapaBase)) {
        val posicion = estado.posicion
        when {
            posicion != null -> {
                mapa(estado.puntos, posicion)
                Column(Modifier.align(Alignment.TopStart).fillMaxWidth()) {
                    CabeceraZona()
                    BadgeSinConexion(
                        estado.minutosSinSenal,
                        Modifier.padding(start = 14.dp, end = 14.dp),
                    )
                }
                HojaArrastrable {
                    ContenidoHojaZona(posicion, estado.puntos)
                }
            }
            estado.cargando -> Box(
                Modifier.fillMaxSize().background(paleta.fondo),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = paleta.accion) }
            else -> Box(
                Modifier.fillMaxSize().background(paleta.fondo),
                contentAlignment = Alignment.Center,
            ) { Text("Sin datos de tu zona todavía", style = EstilosAgua.TituloParada, color = paleta.texto2) }
        }
        AvisoSuperior(estadoAviso, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

@Composable
private fun CabeceraZona(modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    Column(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to paleta.mapaBase.copy(alpha = 0.92f),
                    0.4f to paleta.mapaBase.copy(alpha = 0.92f),
                    1f to Color.Transparent,
                ),
            )
            .statusBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
    ) {
        Text("Tu zona", style = EstilosAgua.Zona, color = paleta.texto)
        // TODO placeholder: no existe todavía un campo "nombre de
        // asociación" expuesto por el backend (la tabla `asociacion` no
        // tiene módulo hexagonal propio, ver auditoría rol Vecino) -- fijo
        // a propósito hasta que se construya esa pieza.
        Text("AA.HH. Villa Solidaridad", style = EstilosAgua.Placa, color = paleta.texto2)
    }
}

@Composable
private fun BadgeSinConexion(minutosSinSenal: Int, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    Row(
        modifier
            .clip(RoundedCornerShape(RadiosAgua.Aviso))
            .background(paleta.alerta.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            TablerIcons.WifiOff, contentDescription = null,
            tint = paleta.alerta, modifier = Modifier.size(16.dp),
        )
        Text(
            "Sin conexión en vivo · hace $minutosSinSenal min",
            style = EstilosAgua.ItemSub, color = paleta.alerta,
        )
    }
}

@Composable
private fun ContenidoHojaZona(posicion: PosicionCisterna, puntos: List<PuntoAbastecimiento>) {
    LazyColumn(
        Modifier.fillMaxSize()
            .padding(horizontal = DimensionesAgua.PaddingHoja)
            .navigationBarsPadding(),
    ) {
        item(key = "litros") {
            Column {
                TarjetaDato(
                    "${formatearLitros(posicion.litrosRestantes)} L",
                    "de ${formatearLitros(posicion.capacidadLitros)} L",
                )
                Spacer(Modifier.height(22.dp))
                RotuloSeccion("Puntos de tu zona")
                Spacer(Modifier.height(4.dp))
            }
        }
        itemsIndexed(puntos, key = { _, p -> p.id }) { indice, p ->
            FichaParada(
                numero = indice + 1,
                titulo = p.nombre,
                subtitulo = "Capacidad: ${formatearLitros(p.capacidadLitros)} L",
                textoDerecha = p.estado.etiqueta,
                // El ViewModel ya filtra a sólo VALIDADO (auditoría rol
                // Vecino) -- ningún otro estado llega hasta acá.
                estado = EstadoFicha.Entregada,
            )
        }
        item(key = "pie") { Spacer(Modifier.height(24.dp)) }
    }
}
