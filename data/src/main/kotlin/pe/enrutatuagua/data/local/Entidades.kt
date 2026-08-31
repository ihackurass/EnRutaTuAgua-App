package pe.enrutatuagua.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// ANDROID-PLAN-04: buffer local de GPS pendiente de publicar por MQTT
// (RNF-01, "guardado local siempre") -- se borra sólo cuando se confirma el
// publish (marcarPublicadas) o cuando envejece/excede el tope (purgarAntiguas).
@Entity(tableName = "posicion_gps_pendiente")
data class PosicionGpsPendienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mensajeId: String,
    val rutaId: Long, val lat: Double, val lng: Double, val registradaEnMillis: Long,
)

@Entity(tableName = "accion_operativa_pendiente")
data class AccionOperativaEntity(
    @PrimaryKey val id: String,
    val tipo: String,
    val rutaId: Long,
    val paradaId: Long,
    val tipoEntrega: String? = null,
    val ajustaA: String? = null,
    val litros: Int? = null,
    val gpsLat: Double? = null,
    val gpsLng: Double? = null,
    val registradaEnMillis: Long? = null,
    val motivoOmision: String? = null,
    val comentario: String? = null,
    val estado: String = "PENDIENTE",
    val error: String? = null,
    val intentos: Int = 0,
    val creadaEnMillis: Long = System.currentTimeMillis(),
)
