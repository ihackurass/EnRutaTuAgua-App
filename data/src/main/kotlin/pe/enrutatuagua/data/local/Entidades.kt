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

@Entity(tableName = "vecino_punto_cache", primaryKeys = ["asociacionId", "puntoId"])
data class VecinoPuntoCacheEntity(
    val asociacionId: Long, val puntoId: Long, val nombre: String,
    val lat: Double, val lng: Double, val capacidadLitros: Int, val estado: String,
)

@Entity(tableName = "vecino_cisterna_cache", primaryKeys = ["asociacionId", "rutaId"])
data class VecinoCisternaCacheEntity(
    val asociacionId: Long, val rutaId: Long, val vehiculoId: Long, val capacidadLitros: Int,
    val lat: Double?, val lng: Double?, val litrosRestantes: Int?, val timestampMs: Long?,
)
