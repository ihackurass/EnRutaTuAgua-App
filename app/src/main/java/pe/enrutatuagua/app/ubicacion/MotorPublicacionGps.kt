package pe.enrutatuagua.app.ubicacion

import android.content.Context
import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.enrutatuagua.domain.repositorio.ConfiguracionTrackingDev
import pe.enrutatuagua.domain.repositorio.PosicionGps
import pe.enrutatuagua.domain.repositorio.PosicionGpsRepository
import pe.enrutatuagua.domain.repositorio.PublicadorPosicionMqtt
import javax.inject.Inject

private const val CICLO_DRENAJE_MS = 30_000L
private const val MAX_POR_DRENAJE = 50

/** RNF-01/RNF-02: este buffer sólo es el colchón de "no perder nada mientras no hay señal" -- la purga de 90 días del trazo durable es un problema del backend. */
private const val TOPE_ANTIGUEDAD_MS = 6L * 60 * 60 * 1000
private const val TOPE_FILAS = 5000

/**
 * Orquesta captura -> guardado local SIEMPRE (RNF-01) -> publish MQTT
 * best-effort (RA-07). Clase Kotlin plana, sin superficie de Service, para
 * que ServicioTrackingGps sea un wrapper delgado y esto se pueda testear sin
 * el ciclo de vida de Android -- mismo criterio que los ViewModels de este
 * repo delegando a use cases en vez de llevar lógica ellos mismos.
 */
class MotorPublicacionGps @Inject constructor(
    private val posicionGpsRepository: PosicionGpsRepository,
    private val publicadorPosicionMqtt: PublicadorPosicionMqtt,
    private val configuracion: ConfiguracionTrackingDev,
) {
    private var capturador: CapturadorPosicionGps? = null
    private var jobDrenaje: Job? = null

    fun iniciar(contexto: Context, alcance: CoroutineScope) {
        val cap = capturador ?: CapturadorPosicionGps(contexto).also { capturador = it }
        cap.iniciar { ubicacion -> alcance.launch { procesarFix(ubicacion) } }
        iniciarDrenajePeriodico(alcance)
    }

    fun detener() {
        capturador?.detener()
        jobDrenaje?.cancel()
        jobDrenaje = null
    }

    fun iniciarDrenajePeriodico(alcance: CoroutineScope) {
        if (jobDrenaje != null) return
        jobDrenaje = alcance.launch {
            while (true) {
                delay(CICLO_DRENAJE_MS)
                drenar()
            }
        }
    }

    suspend fun procesarFix(ubicacion: Location) {
        val posicion = PosicionGps(
            rutaId = configuracion.rutaId,
            lat = ubicacion.latitude,
            lng = ubicacion.longitude,
            registradaEnMillis = System.currentTimeMillis(),
        )
        val guardada = posicionGpsRepository.guardar(posicion) // SIEMPRE, sin importar si el publish sigue
        if (publicadorPosicionMqtt.publicar(configuracion.aguateroId, guardada)) {
            posicionGpsRepository.marcarPublicadas(listOf(guardada.id))
        }
    }

    suspend fun drenar() {
        val pendientes = posicionGpsRepository.pendientes(MAX_POR_DRENAJE)
        val publicadasIds = pendientes
            .filter { publicadorPosicionMqtt.publicar(configuracion.aguateroId, it) }
            .map { it.id }
        posicionGpsRepository.marcarPublicadas(publicadasIds)
        posicionGpsRepository.purgarAntiguas(
            limiteMillis = System.currentTimeMillis() - TOPE_ANTIGUEDAD_MS,
            maxFilas = TOPE_FILAS,
        )
    }
}
