package pe.enrutatuagua.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// ANDROID-PLAN-04: buffer local de GPS pendiente de publicar por MQTT
// (RNF-01, "guardado local siempre") -- se borra sólo cuando se confirma el
// publish (marcarPublicadas) o cuando envejece/excede el tope (purgarAntiguas).
@Entity(tableName = "posicion_gps_pendiente")
data class PosicionGpsPendienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rutaId: Long, val lat: Double, val lng: Double, val registradaEnMillis: Long,
)
