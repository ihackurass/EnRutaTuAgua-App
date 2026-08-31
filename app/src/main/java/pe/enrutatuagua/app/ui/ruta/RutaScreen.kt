package pe.enrutatuagua.app.ui.ruta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.Ban
import pe.enrutatuagua.app.R
import pe.enrutatuagua.app.ubicacion.rememberPermisoNotificaciones
import pe.enrutatuagua.app.ubicacion.rememberPermisoUbicacionSegundoPlano
import pe.enrutatuagua.app.ui.mapa.MapaRuta
import pe.enrutatuagua.designsystem.component.AnclaHoja
import pe.enrutatuagua.designsystem.component.AvisoSuperior
import pe.enrutatuagua.designsystem.component.BotonPrimario
import pe.enrutatuagua.designsystem.component.BotonSecundario
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
import pe.enrutatuagua.domain.repositorio.SaldoJornada
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
    var accionAgua by remember { mutableStateOf<String?>(null) }
    var litrosAgua by remember { mutableStateOf("") }
    if (accionAgua != null) {
        AlertDialog(
            onDismissRequest = { accionAgua = null },
            title = { Text(if (accionAgua == "carga") "Confirmar carga inicial" else "Confirmar recarga") },
            text = {
                Column {
                    Text("Ingresa los litros que cargaste físicamente. El servidor registrará la hora y conservará el movimiento.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = litrosAgua,
                        onValueChange = { litrosAgua = it.filter(Char::isDigit).take(6) },
                        label = { Text("Litros") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = (litrosAgua.toIntOrNull() ?: 0) > 0,
                    onClick = {
                        val litros = litrosAgua.toInt()
                        if (accionAgua == "carga") vm.registrarCarga(litros) else vm.recargar(litros)
                        accionAgua = null; litrosAgua = ""
                    },
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { accionAgua = null }) { Text("Cancelar") } },
        )
    }
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
                val rcReal = rc.copy(litrosRestantes = estado.jornada?.litrosDisponibles ?: rc.litrosRestantes)
                mapa(rcReal.ruta)
                CabeceraRuta(rcReal, Modifier.align(Alignment.TopStart))
                ColumnaDeAgua(
                    litrosRestantes = rcReal.litrosRestantes,
                    capacidadLitros = rcReal.ruta.vehiculo.capacidadTanqueLitros,
                    // Bajo la cabecera: la cifra no debe rozar el chip "En ruta".
                    modifier = Modifier.align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 92.dp, end = 14.dp),
                )
                HojaArrastrable { ancla ->
                    ContenidoHoja(rcReal, ancla, estado.procesandoAccion, onParadaClick, onOmitirClick, vm::cerrarRuta) {
                        accionAgua = "recarga"
                    }
                }
            }
            estado.cargando -> Box(
                Modifier.fillMaxSize().background(paleta.fondo),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = paleta.accion) }
            else -> JornadaSinRuta(
                jornada = estado.jornada,
                procesando = estado.procesandoAccion,
                onCargar = { accionAgua = "carga" },
                onAbrirRuta = vm::abrirRuta,
            )
        }
        AvisoSuperior(
            estadoAviso,
            Modifier.align(Alignment.TopCenter).statusBarsPadding(),
        )
        if (estado.sync.pendientes > 0 || estado.sync.rechazadas > 0) {
            val resumenSync = buildList {
                if (estado.sync.pendientes > 0) add(
                    "${estado.sync.pendientes} ${if (estado.sync.pendientes == 1) "acción pendiente" else "acciones pendientes"} de sincronizar",
                )
                if (estado.sync.rechazadas > 0) add(
                    "${estado.sync.rechazadas} ${if (estado.sync.rechazadas == 1) "acción rechazada" else "acciones rechazadas"}",
                )
            }.joinToString(" · ")
            Surface(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 14.dp).padding(top = 58.dp),
                shape=RoundedCornerShape(RadiosAgua.Aviso), color=paleta.tenue,
            ) {
                Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        resumenSync,
                        style = EstilosAgua.ItemSub,
                        color = if (estado.sync.rechazadas > 0) paleta.error else paleta.texto2,
                        modifier = Modifier.weight(1f),
                    )
                    if (estado.sync.pendientes > 0) {
                        TextButton(onClick = vm::reintentarSync) { Text("Reintentar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun JornadaSinRuta(
    jornada: SaldoJornada?,
    procesando: Boolean,
    onCargar: () -> Unit,
    onAbrirRuta: () -> Unit,
) {
    val p = LocalPaletaAgua.current
    Column(
        Modifier.fillMaxSize().background(p.fondo).statusBarsPadding()
            .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = p.superficie2) {
                Image(
                    painter = painterResource(R.drawable.ill_aguatero_cisterna),
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(42.dp),
                )
            }
            Spacer(Modifier.width(13.dp))
            Column {
                Text("OPERACIÓN DE HOY", style = EstilosAgua.Rotulo, color = p.texto2)
                Text("Tu jornada de reparto", style = EstilosAgua.TituloParada, color = p.texto)
            }
        }
        Spacer(Modifier.height(26.dp))
        Text("Sin ruta abierta", style = EstilosAgua.Rotulo, color = p.texto2)
        Text("Espera la asignación del operador", style = EstilosAgua.ItemSub, color = p.texto2)
        Spacer(Modifier.height(10.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = p.tinta,
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("ESTADO ACTUAL", style = EstilosAgua.Rotulo, color = p.sobreTinta.copy(alpha = .7f))
                Spacer(Modifier.height(13.dp))
                Text(
                    if (jornada == null) "Carga requerida" else "Listo para salir",
                    style = EstilosAgua.TituloParada,
                    color = p.sobreTinta,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    jornada?.let {
                        "${formatearLitros(it.litrosDisponibles)} de ${formatearLitros(it.capacidadLitros)} litros disponibles"
                    } ?: "Registra el agua cargada físicamente antes de iniciar la ruta.",
                    style = EstilosAgua.Cuerpo,
                    color = p.sobreTinta.copy(alpha = .78f),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        RotuloSeccion("Pasos para comenzar")
        Spacer(Modifier.height(10.dp))
        PasoJornada(
            ilustracion = R.drawable.ill_aguatero_recarga,
            titulo = "Cargar la cisterna",
            detalle = if (jornada == null) "Pendiente" else "Carga registrada",
            completado = jornada != null,
        )
        Spacer(Modifier.height(10.dp))
        PasoJornada(
            ilustracion = R.drawable.ill_aguatero_ruta,
            titulo = "Abrir la ruta asignada",
            detalle = if (jornada == null) "Disponible después de la carga" else "Siguiente paso",
            completado = false,
        )
        Spacer(Modifier.weight(1f))
        Text(
            if (jornada == null) "La carga queda registrada con hora y cantidad."
            else "Al abrir la ruta comenzará el seguimiento GPS para los vecinos.",
            style = EstilosAgua.ItemSub,
            color = p.texto2,
        )
        Spacer(Modifier.height(12.dp))
        BotonPrimario(
            texto = if (jornada == null) "Registrar carga inicial" else "Abrir ruta",
            onClick = if (jornada == null) onCargar else onAbrirRuta,
            habilitado = !procesando,
            iconoPainter = painterResource(
                if (jornada == null) R.drawable.ill_aguatero_recarga
                else R.drawable.ill_aguatero_ruta,
            ),
        )
    }
}

@Composable
private fun PasoJornada(@androidx.annotation.DrawableRes ilustracion: Int, titulo: String, detalle: String, completado: Boolean) {
    val p = LocalPaletaAgua.current
    val forma = RoundedCornerShape(12.dp)
    Surface(
        Modifier.fillMaxWidth().border(1.dp, p.linea, forma),
        shape = forma,
        color = p.hoja,
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(ilustracion),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(13.dp))
            Column {
                Text(titulo, style = EstilosAgua.ItemTitulo, color = p.texto)
                Text(detalle, style = EstilosAgua.ItemSub, color = p.texto2)
            }
        }
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
            .padding(start = 14.dp, top = 14.dp, end = 70.dp, bottom = 14.dp),
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
                Image(
                    painter = painterResource(R.drawable.ill_aguatero_cisterna),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("Mi ruta de hoy", style = EstilosAgua.Zona, color = paleta.texto)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_aguatero_gps_vivo),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "En ruta · ${rc.ruta.vehiculo.placa} · $resueltas de ${rc.ruta.paradas.size} paradas",
                            style = EstilosAgua.Placa,
                            color = paleta.accion,
                        )
                    }
                }
            }
        }
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
    onRecargar: () -> Unit,
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
        item(key = "agua-disponible") {
            val forma = RoundedCornerShape(RadiosAgua.Aviso)
            Surface(
                Modifier.fillMaxWidth().border(1.dp, paleta.linea, forma),
                shape = forma,
                color = paleta.hoja,
            ) {
                Row(
                    Modifier.padding(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ill_aguatero_capacidad),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AGUA DISPONIBLE", style = EstilosAgua.Rotulo, color = paleta.texto2)
                        Text(
                            "${formatearLitros(rc.litrosRestantes)} L",
                            style = EstilosAgua.ItemTitulo,
                            color = paleta.texto,
                        )
                    }
                    Button(
                        onClick = onRecargar,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(RadiosAgua.Secundario),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = paleta.superficie2,
                            contentColor = paleta.texto,
                        ),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ill_aguatero_recarga),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Recargar", style = EstilosAgua.BotonPrimario)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
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
                    EstadoEntrega.PARCIAL -> "Faltan ${formatearLitros(p.capacidadEsperadaLitros - p.litrosEntregados)} L"
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
