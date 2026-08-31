package pe.enrutatuagua.app.ui.vecino

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import compose.icons.TablerIcons
import compose.icons.tablericons.WifiOff
import compose.icons.tablericons.Wifi
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
 *
 * <p>Hallazgo real (feedback de producto, verificación manual): antes, sin
 * posición viva, la pantalla mostraba SÓLO un texto ("Sin datos de tu zona
 * todavía") y nunca el mapa -- pero `MapaZonaVecino`/`GeoJsonZonaVecino` ya
 * estaban preparados para `posicion = null` (omiten el marcador del camión,
 * no el mapa en sí). El mapa y los puntos de la zona ahora se muestran
 * SIEMPRE que la carga inicial terminó (`cargando = false`), con o sin
 * posición; un toast avisa la primera vez que llega señal.
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
    // Toast la primera vez que llega señal del camión (o cuando se recupera
    // tras un hueco) -- ya no bloquea el mapa mientras tanto, ver javadoc.
    var teniaPosicionAntes by remember { mutableStateOf(false) }
    LaunchedEffect(estado.posicion) {
        val hayPosicionAhora = estado.posicion != null
        if (hayPosicionAhora && !teniaPosicionAntes) {
            estadoAviso.mostrar("Llegó la posición del camión")
        }
        teniaPosicionAntes = hayPosicionAhora
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
        if (estado.cargando) {
            Box(
                Modifier.fillMaxSize().background(paleta.fondo),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = paleta.accion) }
        } else {
            mapa(estado.puntos, estado.posicion)
            Column(Modifier.align(Alignment.TopStart).fillMaxWidth()) {
                CabeceraZona(estado.asociacionNombre)
                BadgeSinConexion(
                    estado.posicion, estado.minutosSinSenal, estado.estadoTracking,
                    Modifier.padding(start = 14.dp, end = 14.dp),
                )
            }
            HojaArrastrable {
                ContenidoHojaZona(estado.posicion, estado.puntos)
            }
        }
        AvisoSuperior(estadoAviso, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

@Composable
private fun CabeceraZona(asociacionNombre: String, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    Surface(
        modifier
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        shape = RoundedCornerShape(RadiosAgua.Tarjeta),
        color = paleta.hoja,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                "Cisterna en tu zona",
                style = EstilosAgua.Zona,
                color = paleta.texto,
                modifier = Modifier.semantics { heading() },
            )
            Text(asociacionNombre, style = EstilosAgua.Placa, color = paleta.texto2)
        }
    }
}

@Composable
private fun BadgeSinConexion(posicion: PosicionCisterna?, minutosSinSenal: Int, estadoTracking: pe.enrutatuagua.domain.modelo.EstadoTracking, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    // Sin posición nunca "hace N min" no tiene sentido (nunca hubo señal) --
    // distinto de haber tenido una y quedar desactualizada.
    val texto = when {
        posicion == null -> "Esperando la primera posición"
        estadoTracking == pe.enrutatuagua.domain.modelo.EstadoTracking.EN_VIVO -> "En vivo"
        estadoTracking == pe.enrutatuagua.domain.modelo.EstadoTracking.ERROR -> "Error al actualizar · hace $minutosSinSenal min"
        else -> "Respaldo REST · hace $minutosSinSenal min"
    }
    val enVivo = posicion != null && estadoTracking == pe.enrutatuagua.domain.modelo.EstadoTracking.EN_VIVO
    val colorEstado = if (enVivo) paleta.exito else paleta.alerta
    Row(
        modifier
            .clip(RoundedCornerShape(RadiosAgua.Aviso))
            .background(paleta.hoja)
            .border(1.dp, colorEstado, RoundedCornerShape(RadiosAgua.Aviso))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            if (enVivo) TablerIcons.Wifi else TablerIcons.WifiOff,
            contentDescription = null,
            tint = colorEstado, modifier = Modifier.size(16.dp),
        )
        Text(texto, style = EstilosAgua.ItemSub, color = colorEstado)
    }
}

@Composable
private fun ContenidoHojaZona(posicion: PosicionCisterna?, puntos: List<PuntoAbastecimiento>) {
    LazyColumn(
        Modifier.fillMaxSize()
            .padding(horizontal = DimensionesAgua.PaddingHoja)
            .navigationBarsPadding(),
    ) {
        item(key = "encabezado") {
            Column {
                if (posicion != null) {
                    TarjetaDato(
                        "${formatearLitros(posicion.litrosRestantes)} L",
                        "de ${formatearLitros(posicion.capacidadLitros)} L",
                    )
                    Spacer(Modifier.height(22.dp))
                }
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
