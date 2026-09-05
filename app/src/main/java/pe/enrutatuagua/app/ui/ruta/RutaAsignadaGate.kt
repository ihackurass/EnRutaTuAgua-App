package pe.enrutatuagua.app.ui.ruta

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.enrutatuagua.app.R
import pe.enrutatuagua.data.red.mensajeUsuario
import pe.enrutatuagua.designsystem.component.*
import pe.enrutatuagua.designsystem.tokens.*
import pe.enrutatuagua.domain.modelo.*
import pe.enrutatuagua.domain.repositorio.*

data class RutaAsignadaUiState(
    val cargando: Boolean = true,
    val procesando: Boolean = false,
    val operacion: OperacionAguatero? = null,
    val mensaje: String? = null,
)

@HiltViewModel
class RutaAsignadaViewModel @Inject constructor(
    private val repo: OperacionRepository,
    private val rutas: RutaRepository,
    private val sesion: SesionRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(RutaAsignadaUiState())
    val estado: StateFlow<RutaAsignadaUiState> = _estado

    init {
        viewModelScope.launch {
            while (isActive && sesion.rolActual.first() == Rol.AGUATERO) {
                consultarAhora()
                delay(5_000L)
            }
        }
    }

    fun consultar() = viewModelScope.launch { consultarAhora(mostrarCarga = true) }
    fun cargar(litros: Int) = ejecutar {
        val ruta = checkNotNull(_estado.value.operacion?.asignacion?.ruta)
        repo.cargarRuta(ruta.id, UUID.randomUUID().toString(), litros)
    }
    fun iniciar() = ejecutar {
        val ruta = checkNotNull(_estado.value.operacion?.asignacion?.ruta)
        repo.iniciar(ruta.id)
        rutas.invalidarCache()
    }

    private fun ejecutar(accion: suspend () -> Unit) = viewModelScope.launch {
        _estado.value = _estado.value.copy(procesando = true, mensaje = null)
        try {
            accion()
            consultarAhora()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _estado.value = _estado.value.copy(mensaje = e.mensajeUsuario())
        } finally {
            _estado.value = _estado.value.copy(procesando = false)
        }
    }

    private suspend fun consultarAhora(mostrarCarga: Boolean = false) {
        if (mostrarCarga) _estado.value = _estado.value.copy(cargando = true, mensaje = null)
        try {
            val anterior = _estado.value.operacion?.estado
            val actual = repo.operacionAguatero()
            if (actual.estado == EstadoOperacionAguatero.EN_RUTA && anterior != EstadoOperacionAguatero.EN_RUTA) rutas.invalidarCache()
            _estado.value = _estado.value.copy(cargando = false, operacion = actual, mensaje = null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _estado.value = _estado.value.copy(cargando = false, mensaje = e.mensajeUsuario("No se pudo actualizar la asignación"))
        }
    }
}

@Composable
fun RutaAsignadaGate(conRutaAbierta: @Composable () -> Unit, vm: RutaAsignadaViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsState()
    val operacion = estado.operacion
    if (operacion?.estado == EstadoOperacionAguatero.EN_RUTA) {
        conRutaAbierta()
        return
    }
    val p = LocalPaletaAgua.current
    var mostrarCarga by remember { mutableStateOf(false) }
    var litros by remember { mutableStateOf("") }
    if (mostrarCarga) DialogoCargaAgua(
        esCargaInicial = true,
        litros = litros,
        maximoLitros = operacion?.asignacion?.capacidadLitros,
        minimoLitros = operacion?.asignacion?.ruta?.litros ?: 1,
        onLitrosChange = { litros = it.filter(Char::isDigit).take(6) },
        onDismiss = { mostrarCarga = false; litros = "" },
        onConfirm = { vm.cargar(litros.toInt()); mostrarCarga = false; litros = "" },
    )

    Box(Modifier.fillMaxSize().background(p.fondo)) {
        when {
            estado.cargando && operacion == null -> CircularProgressIndicator(color = p.accion, modifier = Modifier.align(Alignment.Center))
            operacion?.asignacion == null -> EsperandoAsignacion(estado.mensaje, estado.cargando, vm::consultar)
            else -> PanelAsignacion(operacion, estado, { mostrarCarga = true }, vm::iniciar)
        }
    }
}

@Composable
private fun PanelAsignacion(
    operacion: OperacionAguatero,
    estado: RutaAsignadaUiState,
    onCargar: () -> Unit,
    onIniciar: () -> Unit,
) {
    val p = LocalPaletaAgua.current
    val asignacion = checkNotNull(operacion.asignacion)
    val ruta = asignacion.ruta
    val esHoy = ruta.fecha == LocalDate.now()
    val saldo = operacion.jornada?.litrosDisponibles ?: 0
    val cargaSuficiente = saldo >= ruta.litros
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ill_aguatero_ruta), null, Modifier.size(54.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("RUTA ASIGNADA", style = EstilosAgua.Rotulo, color = p.texto2)
                Text("Ruta #${ruta.id}", style = EstilosAgua.TituloParada, color = p.texto)
            }
        }
        Spacer(Modifier.height(24.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Hoja), color = p.tinta) {
            Column(Modifier.padding(20.dp)) {
                Text("${ruta.fecha}", style = EstilosAgua.Rotulo, color = p.sobreTinta.copy(alpha = .7f))
                Text("${asignacion.placa} · ${formatearLitros(ruta.litros)} L", style = EstilosAgua.TituloParada, color = p.sobreTinta)
                Text("Capacidad ${formatearLitros(asignacion.capacidadLitros)} L", style = EstilosAgua.Cuerpo, color = p.sobreTinta.copy(alpha = .75f))
            }
        }
        Spacer(Modifier.height(18.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(RadiosAgua.Aviso), color = p.hoja) {
            Column(Modifier.padding(18.dp)) {
                Text("CARGA CONFIRMADA", style = EstilosAgua.Rotulo, color = p.texto2)
                Text("${formatearLitros(saldo)} L", style = EstilosAgua.DatoResumen, color = p.texto)
                Text(when {
                    !esHoy -> "Podrás cargar e iniciar el día programado."
                    operacion.estado == EstadoOperacionAguatero.ASIGNADA -> "Registra el agua cargada físicamente."
                    cargaSuficiente -> "La cisterna está lista para cubrir la ruta."
                    else -> "Faltan ${formatearLitros(ruta.litros - saldo)} L para iniciar."
                }, style = EstilosAgua.Cuerpo, color = if (esHoy && saldo > 0 && !cargaSuficiente) p.alerta else p.texto2)
            }
        }
        estado.mensaje?.let { Spacer(Modifier.height(12.dp)); Text(it, style = EstilosAgua.Error, color = p.error) }
        Spacer(Modifier.weight(1f))
        if (operacion.estado == EstadoOperacionAguatero.ASIGNADA) {
            BotonPrimario("Registrar carga inicial", onClick = onCargar, habilitado = esHoy && !estado.procesando)
        } else {
            BotonPrimario("Iniciar ruta", onClick = onIniciar, habilitado = esHoy && cargaSuficiente && !estado.procesando)
        }
    }
}

@Composable
private fun EsperandoAsignacion(mensaje: String?, procesando: Boolean, onActualizar: () -> Unit) {
    val p = LocalPaletaAgua.current
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.ill_aguatero_ruta), null, Modifier.size(92.dp))
        Spacer(Modifier.height(18.dp))
        Text("Esperando asignación", style = EstilosAgua.TituloParada, color = p.texto)
        Text("El operador todavía no te asignó una ruta. Actualizaremos automáticamente.", style = EstilosAgua.Cuerpo, color = p.texto2)
        mensaje?.let { Spacer(Modifier.height(12.dp)); Text(it, style = EstilosAgua.Error, color = p.error) }
        Spacer(Modifier.height(22.dp))
        BotonSecundario(if (procesando) "Actualizando…" else "Actualizar ahora", onClick = onActualizar)
    }
}
