package pe.enrutatuagua.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ruta")
data class RutaEntity(
    @PrimaryKey val id: Long,
    val fecha: String,               // yyyy-MM-dd
    val estadoCodigo: Int,           // 1=ABIERTA 2=CERRADA
    val vehiculoId: Long,
    val vehiculoPlaca: String,
    val vehiculoCapacidadL: Int,     // cache local del camión del día (subconjunto ER §6)
)

@Entity(tableName = "punto")
data class PuntoEntity(
    @PrimaryKey val id: Long,
    val nombre: String, val lat: Double, val lng: Double, val capacidadLitros: Int,
)

@Entity(tableName = "parada")
data class ParadaEntity(
    @PrimaryKey val id: Long,
    val rutaId: Long, val puntoId: Long, val orden: Int, val capacidadEsperadaL: Int,
    val estadoCodigo: Int, val motivoCodigo: Int?, val comentario: String?,
)

@Entity(tableName = "entrega")
data class EntregaEntity(
    @PrimaryKey val id: String,      // UUID cliente (RN-02)
    val paradaId: Long, val tipoCodigo: Int, val ajustaA: String?,
    val litros: Int, val gpsLat: Double?, val gpsLng: Double?, val registradaEnMillis: Long,
)

// ANDROID-PLAN-04: buffer local de GPS pendiente de publicar por MQTT
// (RNF-01, "guardado local siempre") -- se borra sólo cuando se confirma el
// publish (marcarPublicadas) o cuando envejece/excede el tope (purgarAntiguas).
@Entity(tableName = "posicion_gps_pendiente")
data class PosicionGpsPendienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rutaId: Long, val lat: Double, val lng: Double, val registradaEnMillis: Long,
)
