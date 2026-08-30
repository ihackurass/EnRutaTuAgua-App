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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.Ban
import compose.icons.tablericons.Truck
import pe.enrutatuagua.app.ubicacion.rememberPermisoNotificaciones
import pe.enrutatuagua.app.ubicacion.rememberPermisoUbicacionSegundoPlano
import pe.enrutatuagua.app.ui.mapa.MapaRuta
import pe.enrutatuagua.designsystem.component.AnclaHoja
import pe.enrutatuagua.designsystem.component.AvisoSuperior
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.BotonSecundario
import pe.enrutatuagua.designsystem.component.ChipEstadoRuta
import pe.enrutatuagua.designsystem.component.ColumnaDeAgua
import pe.enrutatuagua.designsystem.component.DeslizadorConfirmar
import pe.enrutatuagua.designsystem.component.EstadoFicha
import pe.enrutatuagua.designsystem.component.FichaParada
import pe.enrutatuagua.designsystem.component.HojaArrastrable
import pe.enrutatuagua.designsystem.component.RotuloSeccion
import pe.enrutatuagua.designsystem.component.rememberEstadoAviso
import pe.enrutatuagua.designsystem.component.TarjetaDato
import pe.enrutatuagua.designsystem.component.formatearLitros
import pe.enrutatuagua.designsystem.tokens.DimensionesAgua
import pe.enrutatuagua.designsystem.tokens.EstilosAgua
import pe.enrutatuagua.designsystem.tokens.LocalPaletaAgua
import pe.enrutatuagua.designsystem.tokens.RadiosAgua
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
    aviso: String? = null,
    onAvisoMostrado: () -> Unit = {},
) {
    val estado by vm.estado.collectAsState()
    val mensaje by vm.mensaje.collectAsState()
    val paleta = LocalPaletaAgua.current
    val estadoAviso = rememberEstadoAviso()
    LaunchedEffect(aviso) {
        aviso?.let {
            estadoAviso.mostrar(it)
            onAvisoMostrado()
        }
    }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            estadoAviso.mostrar(it)
            vm.mensajeMostrado()
        }
    }

    // ANDROID-PLAN-04 (RA-07): sólo se pide con ruta ABIERTA -- no tiene
    // sentido molestar al aguatero con esto si todavía no hay nada que
    // trackear.
    val hayRutaAbierta = estado.rutaConTanque != null
    val permisoSegundoPlano = rememberPermisoUbicacionSegundoPlano()
    val permisoNotificaciones = rememberPermisoNotificaciones()
    var mostrarExplicacionUbicacion by remember { mutableStateOf(false) }
    LaunchedEffect(hayRutaAbierta, permisoSegundoPlano.concedido) {
        if (hayRutaAbierta && !permisoSegundoPlano.concedido) mostrarExplicacionUbicacion = true
    }
    if (mostrarExplicacionUbicacion) {
        DialogoExplicacionUbicacionSegundoPlano(
            onPermitir = {
                mostrarExplicacionUbicacion = false
                permisoNotificaciones.solicitar()
                permisoSegundoPlano.solicitar()
            },
            onDescartar = { mostrarExplicacionUbicacion = false },
        )
    }

    Box(Modifier.fillMaxSize().background(paleta.mapaBase)) {
        val rc = estado.rutaConTanque
        when {
            rc != null -> {
                mapa(rc.ruta)
                CabeceraRuta(rc, Modifier.align(Alignment.TopStart))
                ColumnaDeAgua(
                    litrosRestantes = rc.litrosRestantes,
                    capacidadLitros = rc.ruta.vehiculo.capacidadTanqueLitros,
                    // Bajo la cabecera: la cifra no debe rozar el chip "En ruta".
                    modifier = Modifier.align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 92.dp, end = 14.dp),
                )
                HojaArrastrable { ancla ->
                    ContenidoHoja(rc, ancla, estado.procesandoAccion, onParadaClick, onOmitirClick, vm::cerrarRuta)
                }
            }
            estado.cargando -> Box(
                Modifier.fillMaxSize().background(paleta.fondo),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = paleta.accion) }
            else -> Box(
                Modifier.fillMaxSize().background(paleta.fondo).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sin ruta abierta", style = EstilosAgua.TituloParada, color = paleta.texto2)
                    Spacer(Modifier.height(8.dp))
                    Text("Espera la asignación del operador", style = EstilosAgua.Cuerpo, color = paleta.texto2)
                }
            }
        }
        AvisoSuperior(
            estadoAviso,
            Modifier.align(Alignment.TopCenter).statusBarsPadding(),
        )
    }
}

/** RA-07: "permiso de ubicación en segundo plano con explicación clara" -- este es el "por qué". */
@Composable
private fun DialogoExplicacionUbicacionSegundoPlano(onPermitir: () -> Unit, onDescartar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDescartar,
        title = { Text("Compartir tu ubicación") },
        text = {
            Text(
                "Mientras tengas la ruta abierta, compartimos tu ubicación para que los vecinos " +
                    "vean por dónde va la cisterna -- incluso con la pantalla apagada. Podés " +
                    "desactivarlo cuando quieras desde los ajustes del celular.",
            )
        },
        confirmButton = { TextButton(onClick = onPermitir) { Text("Permitir") } },
        dismissButton = { TextButton(onClick = onDescartar) { Text("Ahora no") } },
    )
}

@Composable
private fun CabeceraRuta(rc: RutaConTanque, modifier: Modifier = Modifier) {
    val paleta = LocalPaletaAgua.current
    val resueltas = rc.ruta.paradas.count { it.estadoEntrega != EstadoEntrega.PENDIENTE }
    // Tarjeta flotante sobre el mapa (patrón Uber), no scrim: legible a pleno sol.
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = RoundedCornerShape(RadiosAgua.Tarjeta),
            color = paleta.hoja,
            shadowElevation = 8.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    TablerIcons.Truck,
                    contentDescription = null,
                    tint = paleta.texto2,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("Mi ruta de hoy", style = EstilosAgua.Zona, color = paleta.texto)
                    Text(
                        "Cisterna ${rc.ruta.vehiculo.placa} · $resueltas de ${rc.ruta.paradas.size} paradas",
                        style = EstilosAgua.Placa, color = paleta.texto2,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        ChipEstadoRuta("En ruta")
    }
}

@Composable
private fun ContenidoHoja(
    rc: RutaConTanque,
    ancla: AnclaHoja,
    procesandoAccion: Boolean,
    onParadaClick: (Long) -> Unit,
    onOmitirClick: (Long) -> Unit,
    onCerrarRuta: () -> Unit,
) {
    val paleta = LocalPaletaAgua.current
    val proxima = rc.ruta.paradas.firstOrNull { it.estadoEntrega == EstadoEntrega.PENDIENTE }
    val entregadoTotal = rc.ruta.vehiculo.capacidadTanqueLitros - rc.litrosRestantes
    val omitidas = rc.ruta.paradas.count { it.estadoEntrega == EstadoEntrega.OMITIDO }

    LazyColumn(
        Modifier.fillMaxSize()
            .padding(horizontal = DimensionesAgua.PaddingHoja)
            .navigationBarsPadding(),
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
                    BotonSecundario(
                        "No pude entregar",
                        onClick = { onOmitirClick(proxima.id) },
                        icono = TablerIcons.Ban,
                    )
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
                    Spacer(Modifier.height(14.dp))
                    BotonPrimario("Cerrar ruta", onClick = onCerrarRuta, habilitado = !procesandoAccion)
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
