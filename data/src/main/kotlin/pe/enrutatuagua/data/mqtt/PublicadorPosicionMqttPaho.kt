package pe.enrutatuagua.data.mqtt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttMessage
import pe.enrutatuagua.domain.repositorio.ConfiguracionTrackingDev
import pe.enrutatuagua.domain.repositorio.PosicionGps
import pe.enrutatuagua.domain.repositorio.PublicadorPosicionMqtt
import java.util.UUID
import javax.inject.Inject

private const val TIMEOUT_MS = 5_000L

/**
 * Sólo PUBLICA -- nunca se suscribe -- así que no hereda el bug de Paho
 * 1.2.5 con subscribe()+listener confirmado del lado backend (ver
 * CallbackReconexionMqtt.java). Conexión persistente y perezosa (se conecta
 * en el primer publish; reconexión automática) en vez de conectar/
 * desconectar por mensaje -- mismo espíritu que ConfiguracionMqtt/
 * ConectorMqtt del backend. `token.waitForCompletion(timeout)` sobre
 * Dispatchers.IO: el mismo patrón bloqueante-sobre-token que ya se confirmó
 * empíricamente contra un broker real esta sesión (backend), no un callback
 * async nuevo sin probar. best-effort no bloqueante (RA-07): cualquier
 * fallo (conexión caída, timeout, broker inalcanzable) devuelve false, nunca
 * propaga -- el llamador reintenta más tarde desde el buffer local.
 */
class PublicadorPosicionMqttPaho @Inject constructor(
    private val configuracion: ConfiguracionTrackingDev,
) : PublicadorPosicionMqtt {

    private val cliente by lazy {
        MqttAsyncClient(configuracion.brokerUrl, "enrutatuagua-android-${UUID.randomUUID()}", MemoryPersistence())
    }

    override suspend fun publicar(aguateroId: Long, posicion: PosicionGps): Boolean =
        withTimeoutOrNull(TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                try {
                    if (!cliente.isConnected) {
                        val opciones = MqttConnectionOptions().apply {
                            setAutomaticReconnect(true)
                            if (configuracion.password.isNotBlank()) {
                                userName = "aguatero-$aguateroId"
                                password = configuracion.password.toByteArray()
                            }
                        }
                        cliente.connect(opciones).waitForCompletion(TIMEOUT_MS)
                    }
                    val mensaje = MqttMessage(PayloadGpsMqtt.json(posicion).toByteArray())
                    mensaje.qos = 1
                    cliente.publish(PayloadGpsMqtt.topico(aguateroId), mensaje).waitForCompletion(TIMEOUT_MS)
                    true
                } catch (e: Exception) {
                    false
                }
            }
        } ?: false
}
