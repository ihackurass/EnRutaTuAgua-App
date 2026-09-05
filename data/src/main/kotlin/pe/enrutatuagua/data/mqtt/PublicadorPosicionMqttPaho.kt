package pe.enrutatuagua.data.mqtt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CompletableDeferred
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttMessage
import pe.enrutatuagua.domain.repositorio.ConfiguracionTrackingDev
import pe.enrutatuagua.domain.repositorio.PosicionGps
import pe.enrutatuagua.domain.repositorio.PublicadorPosicionMqtt
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import pe.enrutatuagua.domain.repositorio.ResultadoAckGps
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

    private val esperasAck = ConcurrentHashMap<String, CompletableDeferred<ResultadoAckGps>>()

    private val cliente by lazy {
        MqttAsyncClient(configuracion.brokerUrl, "enrutatuagua-android-${UUID.randomUUID()}", MemoryPersistence()).apply {
            setCallback(object : MqttCallback {
                override fun connectComplete(reconnect:Boolean, serverURI:String?) = Unit
                override fun disconnected(disconnectResponse:MqttDisconnectResponse?) = Unit
                override fun mqttErrorOccurred(exception:MqttException?) = Unit
                override fun deliveryComplete(token:IMqttToken?) = Unit
                override fun authPacketArrived(reasonCode:Int, properties:MqttProperties?) = Unit
                override fun messageArrived(topic:String?, message:MqttMessage?) {
                    val texto=message?.payload?.toString(Charsets.UTF_8) ?: return
                    val id=Regex("\\\"mensajeId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(texto)?.groupValues?.get(1) ?: return
                    val estado=Regex("\\\"estado\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(texto)?.groupValues?.get(1)
                    val motivo=Regex("\\\"motivo\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").find(texto)?.groupValues?.get(1)
                    esperasAck.remove(id)?.complete(if(estado=="ACEPTADA") ResultadoAckGps.Aceptada else ResultadoAckGps.Rechazada(motivo))
                }
            })
        }
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

    override suspend fun publicarYEsperarAck(aguateroId:Long,posicion:PosicionGps):ResultadoAckGps {
        val espera=CompletableDeferred<ResultadoAckGps>();esperasAck[posicion.mensajeId]=espera
        val publicado=withTimeoutOrNull(TIMEOUT_MS){withContext(Dispatchers.IO){try{
            conectar(aguateroId)
            cliente.subscribe(PayloadGpsMqtt.topicoAck(aguateroId),1).waitForCompletion(TIMEOUT_MS)
            val mensaje=MqttMessage(PayloadGpsMqtt.json(posicion).toByteArray()).apply{qos=1}
            cliente.publish(PayloadGpsMqtt.topico(aguateroId),mensaje).waitForCompletion(TIMEOUT_MS);true
        }catch(_:Exception){false}}}?:false
        if(!publicado){esperasAck.remove(posicion.mensajeId);return ResultadoAckGps.SinRespuesta}
        return withTimeoutOrNull(8_000){espera.await()} ?: ResultadoAckGps.SinRespuesta.also{esperasAck.remove(posicion.mensajeId)}
    }

    private fun conectar(aguateroId:Long){
        if(cliente.isConnected)return
        val opciones=MqttConnectionOptions().apply{setAutomaticReconnect(true);if(configuracion.password.isNotBlank()){userName="aguatero-$aguateroId";password=configuracion.password.toByteArray()}}
        cliente.connect(opciones).waitForCompletion(TIMEOUT_MS)
    }
}
