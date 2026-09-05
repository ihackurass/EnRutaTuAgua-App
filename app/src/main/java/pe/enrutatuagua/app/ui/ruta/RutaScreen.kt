package pe.enrutatuagua.app.ui.ruta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.SolidColor
import androidx.hilt.navigation.compose.hiltViewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.Ban
import compose.icons.tablericons.AlertTriangle
import compose.icons.tablericons.CloudUpload
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
import pe.enrutatuagua.domain.modelo.FuenteCarga
import pe.enrutatuagua.domain.modelo.SolicitudRecarga
import pe.enrutatuagua.domain.modelo.EstadoSolicitudRecarga
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
            estadoAviso.mostrar(it, error = true)
            onAvisoMostrado()
        }
    }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            estadoAviso.mostrar(it)
            vm.mensajeMostrado()
        }
    }
    LaunchedEffect(vm) {
        while (true) {
            vm.refrescarCustodia()
            delay(5_000L)
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
    var solicitarRecarga by remember { mutableStateOf(false) }
    var reportarCarga by remember { mutableStateOf(false) }
    var cerrarRuta by remember { mutableStateOf(false) }
    accionAgua?.let { accion ->
        val maximoRecargable = estado.jornada?.let { (it.capacidadLitros - it.litrosDisponibles).coerceAtLeast(0) }
        DialogoCargaAgua(
            esCargaInicial = accion == "carga",
            litros = litrosAgua,
            maximoLitros = if (accion == "recarga") maximoRecargable else null,
            onLitrosChange = { litrosAgua = it.filter(Char::isDigit).take(6) },
            onDismiss = { accionAgua = null; litrosAgua = "" },
            onConfirm = {
                val litros = litrosAgua.toInt()
                if (accion == "carga") vm.registrarCarga(litros)
                accionAgua = null; litrosAgua = ""
            },
        )
    }
    if (solicitarRecarga) DialogoSolicitudRecarga(
        fuentes = estado.fuentes,
        maximoLitros = estado.jornada?.let { (it.capacidadLitros-it.litrosDisponibles).coerceAtLeast(0) } ?: 0,
        onDismiss = { solicitarRecarga=false },
        onConfirm = { fuente,litros,motivo -> vm.solicitarRecarga(fuente,litros,motivo); solicitarRecarga=false },
    )
    if (reportarCarga) DialogoReportarCarga(
        maximoLitros = estado.solicitudRecarga?.litrosSolicitados ?: 0,
        onDismiss = { reportarCarga=false },
        onConfirm = { vm.reportarCarga(it); reportarCarga=false },
    )
    if (cerrarRuta) DialogoCerrarConciliando(
        saldoTeorico = estado.jornada?.litrosDisponibles ?: 0,
        onDismiss = { cerrarRuta=false },
        onConfirm = { vm.cerrarRuta(it); cerrarRuta=false },
    )
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
                    ContenidoHoja(rcReal, ancla, estado.procesandoAccion, estado.sync, estado.solicitudRecarga,
                        onParadaClick, onOmitirClick, { cerrarRuta=true }, vm::reintentarSync, vm::reconocerRechazadas,
                        { solicitarRecarga=true }, { reportarCarga=true })
                }
            }
            estado.cargando -> Box(
                Modifier.fillMaxSize().background(paleta.fondo),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = paleta.accion) }
            else -> JornadaSinRuta(jornada = estado.jornada)
        }
        AvisoSuperior(estadoAviso, Modifier.align(Alignment.Center))
        if (estado.sync.pendientes > 0 || estado.sync.rechazadas > 0) {
            val hayRechazadas = estado.sync.rechazadas > 0
            val cantidad = if (hayRechazadas) estado.sync.rechazadas else estado.sync.pendientes
            val color = if (hayRechazadas) paleta.error else paleta.alerta
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()
                    .padding(top = 13.dp, end = 58.dp)
                    .size(34.dp)
                    .let { base -> if (!hayRechazadas) base.clickable(onClick = vm::reintentarSync) else base },
                shape = RoundedCornerShape(11.dp),
                color = color,
                shadowElevation = 5.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (hayRechazadas) TablerIcons.AlertTriangle else TablerIcons.CloudUpload,
                        contentDescription = if (hayRechazadas) "$cantidad acciones rechazadas" else "$cantidad acciones pendientes; tocar para reintentar",
                        tint = paleta.hoja,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        cantidad.coerceAtMost(9).let { if (cantidad > 9) "9+" else it.toString() },
                        style = EstilosAgua.PieColumna,
                        color = paleta.hoja,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun DialogoCargaAgua(
    esCargaInicial: Boolean,
    litros: String,
    maximoLitros: Int?,
    minimoLitros: Int = 1,
    onLitrosChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val p = LocalPaletaAgua.current
    val cantidad = litros.toIntOrNull() ?: 0
    val excedeCapacidad = maximoLitros != null && cantidad > maximoLitros
    val faltaCarga = cantidad in 1 until minimoLitros
    val puedeConfirmar = cantidad >= minimoLitros && !excedeCapacidad
    val formaCampo = RoundedCornerShape(16.dp)
    val opcionesRapidas = buildList {
        add(1_000)
        add(3_000)
        maximoLitros?.takeIf { it > 0 }?.let(::add)
    }.distinct().filter { maximoLitros == null || it <= maximoLitros }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.hoja,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = p.superficie2) {
                    Image(
                        painter = painterResource(R.drawable.ill_aguatero_recarga),
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(38.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (esCargaInicial) "Registrar carga" else "Recargar cisterna",
                        style = EstilosAgua.TituloParada,
                        color = p.texto,
                    )
                    Text("MOVIMIENTO DE AGUA", style = EstilosAgua.Rotulo, color = p.texto2)
                }
            }
        },
        text = {
            Column {
                Text(
                    "Ingresa la cantidad cargada físicamente.",
                    style = EstilosAgua.Cuerpo,
                    color = p.texto2,
                )
                Spacer(Modifier.height(16.dp))
                Text("LITROS CARGADOS", style = EstilosAgua.Rotulo, color = p.texto2)
                Spacer(Modifier.height(7.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().border(
                        width = 2.dp,
                        color = if (excedeCapacidad || faltaCarga) p.error else if (litros.isNotEmpty()) p.accion else p.linea,
                        shape = formaCampo,
                    ),
                    shape = formaCampo,
                    color = p.fondo,
                ) {
                    BasicTextField(
                        value = litros,
                        onValueChange = onLitrosChange,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp),
                        textStyle = EstilosAgua.DatoResumen.copy(color = p.texto),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        cursorBrush = SolidColor(p.accion),
                        decorationBox = { campo ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    if (litros.isEmpty()) Text("0", style = EstilosAgua.DatoResumen, color = p.texto2)
                                    campo()
                                }
                                Text("L", style = EstilosAgua.UnidadCifra, color = p.texto2)
                            }
                        },
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    when {
                        excedeCapacidad -> "Solo caben ${formatearLitros(maximoLitros!!)} L adicionales."
                        faltaCarga -> "La ruta requiere al menos ${formatearLitros(minimoLitros)} L."
                        maximoLitros != null -> "Espacio disponible: ${formatearLitros(maximoLitros)} L"
                        else -> "La cantidad quedará registrada con fecha y hora."
                    },
                    style = if (excedeCapacidad || faltaCarga) EstilosAgua.Error else EstilosAgua.ItemSub,
                    color = if (excedeCapacidad || faltaCarga) p.error else p.texto2,
                )
                if (opcionesRapidas.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("SELECCIÓN RÁPIDA", style = EstilosAgua.Rotulo, color = p.texto2)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        opcionesRapidas.forEach { opcion ->
                            val elegida = cantidad == opcion
                            Surface(
                                modifier = Modifier.heightIn(min = 48.dp).clickable { onLitrosChange(opcion.toString()) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (elegida) p.tinta else p.superficie2,
                                contentColor = if (elegida) p.sobreTinta else p.texto,
                            ) {
                                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        if (maximoLitros == opcion) "Completar" else "${formatearLitros(opcion)} L",
                                        style = EstilosAgua.Preset,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            BotonPrimario(
                texto = if (esCargaInicial) "Registrar carga" else "Confirmar recarga",
                onClick = onConfirm,
                habilitado = puedeConfirmar,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancelar", style = EstilosAgua.BotonPrimario, color = p.texto2)
            }
        },
    )
}

@Composable
private fun JornadaSinRuta(
    jornada: SaldoJornada?,
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
        Text("Actualizando el estado de la asignación…", style = EstilosAgua.ItemSub, color = p.texto2)
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
private fun DialogoSolicitudRecarga(
    fuentes:List<FuenteCarga>,maximoLitros:Int,onDismiss:()->Unit,onConfirm:(Long,Int,String)->Unit,
) {
    var fuenteId by remember { mutableStateOf(fuentes.firstOrNull()?.id) }
    var litros by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    val cantidad=litros.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Solicitar recarga")},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("El operador llamará a la fuente antes y después de la carga.",style=EstilosAgua.Cuerpo)
            Text("FUENTE AUTORIZADA",style=EstilosAgua.Rotulo)
            fuentes.forEach { f -> FilterChip(selected=fuenteId==f.id,onClick={fuenteId=f.id},label={Text(f.nombre)}) }
            OutlinedTextField(value=litros,onValueChange={litros=it.filter(Char::isDigit).take(6)},
                label={Text("Litros solicitados")},supportingText={Text("Espacio disponible: ${formatearLitros(maximoLitros)} L")},
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            OutlinedTextField(value=motivo,onValueChange={motivo=it.take(250)},label={Text("Motivo de la recarga")},
                supportingText={Text("Mínimo 10 caracteres")},minLines=2)
        }},
        confirmButton={TextButton(enabled=fuenteId!=null&&cantidad in 1..maximoLitros&&motivo.trim().length>=10,
            onClick={onConfirm(checkNotNull(fuenteId),cantidad,motivo.trim())}){Text("Enviar solicitud")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}},
    )
}

@Composable
private fun DialogoReportarCarga(maximoLitros:Int,onDismiss:()->Unit,onConfirm:(Int)->Unit) {
    var litros by remember { mutableStateOf(maximoLitros.toString()) }
    val cantidad=litros.toIntOrNull() ?: 0
    AlertDialog(onDismissRequest=onDismiss,title={Text("Reportar carga realizada")},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("Indica los litros que ingresaron físicamente. El saldo cambiará cuando el operador haga la segunda llamada.")
            OutlinedTextField(value=litros,onValueChange={litros=it.filter(Char::isDigit).take(6)},
                label={Text("Litros cargados")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
        }},confirmButton={TextButton(enabled=cantidad in 1..maximoLitros,onClick={onConfirm(cantidad)}){Text("Reportar")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Volver")}})
}

@Composable
private fun DialogoCerrarConciliando(saldoTeorico:Int,onDismiss:()->Unit,onConfirm:(Int)->Unit) {
    var litros by remember { mutableStateOf(saldoTeorico.coerceAtLeast(0).toString()) }
    val cantidad=litros.toIntOrNull()
    AlertDialog(onDismissRequest=onDismiss,title={Text("Conciliar y cerrar")},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("Saldo calculado: ${formatearLitros(saldoTeorico)} L. Ingresa la lectura física real del tanque.")
            OutlinedTextField(value=litros,onValueChange={litros=it.filter(Char::isDigit).take(6)},
                label={Text("Litros físicos restantes")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            Text("Una diferencia mayor a 50 L abrirá una incidencia y bloqueará la cisterna.",style=EstilosAgua.ItemSub)
        }},confirmButton={TextButton(enabled=cantidad!=null,onClick={onConfirm(checkNotNull(cantidad))}){Text("Cerrar ruta")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}

@Composable
private fun TarjetaSolicitudRecarga(s:SolicitudRecarga,onReportar:()->Unit) {
    val p=LocalPaletaAgua.current
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(RadiosAgua.Aviso),color=p.tenue){
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Text("RECARGA · ${s.estado.name.replace('_',' ')}",style=EstilosAgua.Rotulo,color=p.accion)
            Text("${s.fuenteNombre} · ${formatearLitros(s.litrosSolicitados)} L",style=EstilosAgua.ItemTitulo,color=p.texto)
            Text(when(s.estado){
                EstadoSolicitudRecarga.SOLICITADA->"Esperando que el operador llame y autorice."
                EstadoSolicitudRecarga.AUTORIZADA->"Autorizada. Realiza la carga y reporta los litros reales."
                EstadoSolicitudRecarga.CARGA_REPORTADA->"Esperando la segunda llamada de confirmación."
                else->s.estado.name.replace('_',' ')
            },style=EstilosAgua.ItemSub,color=p.texto2)
            if(s.estado==EstadoSolicitudRecarga.AUTORIZADA) TextButton(onClick=onReportar){Text("Reportar carga realizada")}
        }
    }
}

@Composable
private fun ContenidoHoja(
    rc: RutaConTanque,
    ancla: AnclaHoja,
    procesandoAccion: Boolean,
    sync: pe.enrutatuagua.domain.modelo.EstadoSyncOperativo,
    solicitudRecarga: SolicitudRecarga?,
    onParadaClick: (Long) -> Unit,
    onOmitirClick: (Long) -> Unit,
    onCerrarRuta: () -> Unit,
    onReintentarSync: () -> Unit,
    onReconocerRechazadas: () -> Unit,
    onRecargar: () -> Unit,
    onReportarCarga: () -> Unit,
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
                        enabled = solicitudRecarga == null,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(RadiosAgua.Secundario),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = paleta.superficie2,
                            contentColor = paleta.texto,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ill_aguatero_recarga),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Recarga", style = EstilosAgua.BotonPrimario)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        solicitudRecarga?.let { solicitud -> item(key="solicitud-recarga") {
            TarjetaSolicitudRecarga(solicitud,onReportarCarga)
            Spacer(Modifier.height(12.dp))
        } }
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
                    if (sync.pendientes > 0 || sync.rechazadas > 0) {
                        Text(
                            if (sync.rechazadas > 0) "Revisa las acciones rechazadas antes de cerrar."
                            else "Sincroniza ${sync.pendientes} acciones pendientes antes de cerrar.",
                            style = EstilosAgua.Error,
                            color = if (sync.rechazadas > 0) paleta.error else paleta.alerta,
                        )
                        Spacer(Modifier.height(8.dp))
                        BotonSecundario(
                            if (sync.rechazadas > 0) "Reconocer rechazo" else "Sincronizar ahora",
                            onClick = if (sync.rechazadas > 0) onReconocerRechazadas else onReintentarSync,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    BotonPrimario(
                        "Cerrar ruta",
                        onClick = onCerrarRuta,
                        habilitado = !procesandoAccion && sync.pendientes == 0 && sync.rechazadas == 0,
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
