package pe.enrutatuagua.app.ui.ruta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.app.ui.mapa.MapaRuta
import pe.enrutatuagua.designsystem.component.AnclaHoja
import pe.enrutatuagua.designsystem.component.BotonSecundario
import pe.enrutatuagua.designsystem.component.ColumnaDeAgua
import pe.enrutatuagua.designsystem.component.DeslizadorConfirmar
import pe.enrutatuagua.designsystem.component.EstadoFicha
import pe.enrutatuagua.designsystem.component.FichaParada
import pe.enrutatuagua.designsystem.component.HojaArrastrable
import pe.enrutatuagua.designsystem.component.RotuloSeccion
import pe.enrutatuagua.designsystem.component.TarjetaDato
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.modelo.Ruta
import pe.enrutatuagua.domain.usecase.RutaConTanque

/**
 * Mi ruta (plan 07 T6): el mapa es el lienzo; cabecera con gradiente, columna de
 * agua a la derecha y hoja arrastrable con la siguiente parada + lista del día.
 * El mapa entra por slot para poder testear la pantalla sin MapLibre.
 */
@Composable
fun RutaScreen(
    onParadaClick: (Long) -> Unit,
    onOmitirClick: (Long) -> Unit,
    vm: RutaViewModel = hiltViewModel(),
    mapa: @Composable (Ruta) -> Unit = { MapaRuta(it, Modifier.fillMaxSize()) },
) {
    val estado by vm.estado.collectAsState()
    val paleta = LocalPaletaAgua.current

    Box(Modifier.fillMaxSize().background(paleta.mapaBase)) {
        val rc = estado.rutaConTanque
        when {
            rc != null -> {
                mapa(rc.ruta)
                CabeceraRuta(rc, Modifier.align(Alignment.TopStart))
                ColumnaDeAgua(
                    litrosRestantes = rc.litrosRestantes,
                    capacidadLitros = rc.ruta.vehiculo.capacidadTanqueLitros,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 14.dp),
                )
                HojaArrastrable { ancla ->
                    ContenidoHoja(rc, ancla, onParadaClick, onOmitirClick)
                }
            }
            estado.cargando -> Box(
                Modifier.fillMaxSize().background(paleta.fondo),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = paleta.accion) }
            else -> Box(
                Modifier.fillMaxSize().background(paleta.fondo),
                contentAlignment = Alignment.Center,
            ) { Text("Sin ruta abierta", style = EstilosAgua.TituloParada, color = paleta.texto2) }
        }
    }
}

@Composable
private fun CabeceraRuta(rc: RutaConTanque, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    val resueltas = rc.ruta.paradas.count { it.estadoEntrega != EstadoEntrega.PENDIENTE }
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
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 26.dp),
    ) {
        Text("Mi ruta de hoy", style = EstilosAgua.Zona, color = paleta.texto)
        Text(
            "Cisterna ${rc.ruta.vehiculo.placa} · $resueltas de ${rc.ruta.paradas.size} paradas",
            style = EstilosAgua.Placa, color = paleta.texto2,
        )
    }
}

@Composable
private fun ContenidoHoja(
    rc: RutaConTanque,
    ancla: AnclaHoja,
    onParadaClick: (Long) -> Unit,
    onOmitirClick: (Long) -> Unit,
) {
    val paleta = LocalPaletaAgua.current
    val proxima = rc.ruta.paradas.firstOrNull { it.estadoEntrega == EstadoEntrega.PENDIENTE }
    val entregadoTotal = rc.ruta.vehiculo.capacidadTanqueLitros - rc.litrosRestantes
    val omitidas = rc.ruta.paradas.count { it.estadoEntrega == EstadoEntrega.OMITIDO }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = DimensionesAgua.PaddingHoja),
        userScrollEnabled = ancla == AnclaHoja.Abierta,
    ) {
        if (proxima != null) {
            item(key = "siguiente") {
                Column {
                    RotuloSeccion("Siguiente parada", acento = "#${proxima.orden}")
                    Spacer(Modifier.height(6.dp))
                    Text(proxima.punto.nombre, style = EstilosAgua.TituloParada, color = paleta.texto)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "pidió ${formatearLitros(proxima.capacidadEsperadaLitros)} L",
                        style = EstilosAgua.Cuerpo, color = paleta.texto2,
                    )
                    Spacer(Modifier.height(14.dp))
                    DeslizadorConfirmar("Desliza para entregar", onConfirmar = { onParadaClick(proxima.id) })
                    Spacer(Modifier.height(10.dp))
                    BotonSecundario("No pude entregar", onClick = { onOmitirClick(proxima.id) })
                }
            }
        } else {
            item(key = "resumen") {
                Column {
                    RotuloSeccion("Sin paradas pendientes")
                    Spacer(Modifier.height(6.dp))
                    Text("Ruta lista para cerrar", style = EstilosAgua.TituloParada, color = paleta.texto)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TarjetaDato(formatearLitros(entregadoTotal), "Litros entregados", Modifier.weight(1f))
                        TarjetaDato("$omitidas", "Paradas omitidas", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "El cierre de ruta llega con el plan 02",
                        style = EstilosAgua.ItemSub, color = paleta.texto2,
                    )
                }
            }
        }

        item(key = "rotulo-lista") {
            Column {
                Spacer(Modifier.height(22.dp))
                RotuloSeccion("Paradas de hoy")
                Spacer(Modifier.height(4.dp))
            }
        }

        items(rc.ruta.paradas, key = { it.id }) { p ->
            val esProxima = proxima?.id == p.id
            FichaParada(
                numero = p.orden,
                titulo = p.punto.nombre,
                subtitulo = "Esperado: ${formatearLitros(p.capacidadEsperadaLitros)} L",
                textoDerecha = when (p.estadoEntrega) {
                    EstadoEntrega.ENTREGADO -> "Entregada"
                    EstadoEntrega.OMITIDO -> p.motivoOmision?.etiqueta ?: "Omitida"
                    EstadoEntrega.PENDIENTE -> "${formatearLitros(p.capacidadEsperadaLitros)} L"
                },
                estado = when {
                    p.estadoEntrega == EstadoEntrega.ENTREGADO -> EstadoFicha.Entregada
                    p.estadoEntrega == EstadoEntrega.OMITIDO -> EstadoFicha.Omitida
                    esProxima -> EstadoFicha.Proxima
                    else -> EstadoFicha.Pendiente
                },
                onClick = if (p.estadoEntrega == EstadoEntrega.PENDIENTE) {
                    { onParadaClick(p.id) }
                } else null,
            )
        }

        item(key = "pie") { Spacer(Modifier.height(24.dp)) }
    }
}
