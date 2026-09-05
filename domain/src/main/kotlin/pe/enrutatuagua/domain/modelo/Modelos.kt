package pe.enrutatuagua.domain.modelo

import java.time.LocalDate

data class Sesion(
    val usuarioId: Long,
    val roles: Set<Rol>,
    val asociacionId: Long?,
    val rolActivo: Rol?,
    val token: String,
    val expiraEn: Long,
    val empresaId: Long? = null,
)

// Códigos idénticos a sql/01-esquema.sql (catalogo_valor)
enum class Rol(val codigo: Int, val etiqueta: String) {
    AGUATERO(1, "Aguatero (cisterna)"), VECINO(2, "Vecino"),
    PRESIDENTE(3, "Presidente de asociación"), ADMIN(4, "Administrador"),
    OPERADOR(5, "Operador de empresa")
}
enum class EstadoRuta(val codigo: Int) { ABIERTA(1), CERRADA(2), PLANIFICADA(3), CANCELADA(4) }
enum class EstadoEntrega(val codigo: Int) { PENDIENTE(1), ENTREGADO(2), OMITIDO(3), PARCIAL(4) }
enum class MotivoOmision(val codigo: Int, val etiqueta: String) {
    CALLE_CERRADA(1, "Calle cerrada"), NADIE_PRESENTE(2, "Nadie presente"),
    FALTA_AGUA(3, "Falta de agua"), OTRO(4, "Otro")
}
enum class TipoEntrega(val codigo: Int) { ENTREGA(1), AJUSTE(2) }

// Códigos idénticos a sql/01-esquema.sql (catalogo_valor)
enum class EstadoValidacionPunto(val codigo: Int, val etiqueta: String) {
    PROPUESTO(1, "Propuesto"), VALIDADO(2, "Validado"),
    RECHAZADO(3, "Rechazado"), SUSPENDIDO(4, "Suspendido")
}
enum class EstadoPedido(val codigo: Int, val etiqueta: String) {
    PENDIENTE(1, "Pendiente"), EN_RUTA(2, "En ruta"), ATENDIDO(3, "Atendido"),
    CANCELADO(4, "Cancelado"), RETENIDO(5, "Retenido"), PROGRAMADO(6, "Programado"),
    PARCIAL(7, "Atendido parcialmente")
}

data class LimitePedido(val capacidadMaximaCisternaLitros: Int, val maxLitrosPedido: Int, val tienePedidoActivo: Boolean)
data class AsociacionOperacion(val id: Long, val nombre: String)
data class VehiculoOperacion(val id: Long, val placa: String, val capacidadLitros: Int,
    val conductorId: Long? = null, val disponible: Boolean = true, val motivoNoDisponible: String? = null)
data class AguateroOperacion(val id: Long, val nombre: String,
    val disponible: Boolean = true, val motivoNoDisponible: String? = null)
data class PedidoOperacion(val id: Long, val pedidoItemId: Long, val puntoId: Long, val puntoNombre: String, val litros: Int)
data class ResumenOperacion(val asociaciones: List<AsociacionOperacion>, val vehiculos: List<VehiculoOperacion>,
    val aguateros: List<AguateroOperacion>, val fecha: LocalDate = LocalDate.now())
data class SugerenciaRuta(val capacidadLitros: Int, val litrosSugeridos: Int, val sugeridos: List<PedidoOperacion>, val disponibles: List<PedidoOperacion>)
data class RutaPlanificada(val id: Long, val fecha: LocalDate, val asociacionId: Long, val vehiculoId: Long, val aguateroId: Long, val litros: Int, val estado: EstadoRuta)
enum class EstadoOperacionAguatero { SIN_ASIGNACION, ASIGNADA, CARGA_CONFIRMADA, EN_RUTA }
data class RutaAsignadaAguatero(val ruta: RutaPlanificada, val placa: String, val capacidadLitros: Int)
data class RutaOperacion(val id: Long, val fecha: LocalDate, val estado: EstadoRuta,
    val asociacion: AsociacionOperacion, val vehiculo: VehiculoOperacion,
    val aguatero: AguateroOperacion, val litros: Int, val paradas: Int)
data class EventoOperacion(val id: Long, val categoria: String, val referencia: String,
    val accion: String, val detalle: String, val ocurridoEn: Long, val actorNombre: String?)

data class Vehiculo(val id: Long, val placa: String, val capacidadTanqueLitros: Int)
data class Punto(val id: Long, val nombre: String, val lat: Double, val lng: Double, val capacidadLitros: Int)
data class Parada(
    val id: Long, val punto: Punto, val orden: Int, val capacidadEsperadaLitros: Int,
    val estadoEntrega: EstadoEntrega, val motivoOmision: MotivoOmision?, val comentarioOmision: String?,
    val litrosEntregados: Int = 0,
)

enum class EstadoSolicitudRecarga {
    SOLICITADA, AUTORIZADA, CARGA_REPORTADA, CONFIRMADA, RECHAZADA, CANCELADA, VENCIDA;
    companion object { fun desdeCodigo(codigo: Int) = entries[codigo - 1] }
}

data class FuenteCarga(
    val id: Long,
    val nombre: String,
    val lat: Double,
    val lng: Double,
    val radioPermitidoMetros: Int,
    val contactoNombre: String? = null,
    val contactoTelefono: String? = null,
)

data class SolicitudRecarga(
    val id: String,
    val jornadaId: Long,
    val rutaId: Long,
    val fuenteId: Long,
    val fuenteNombre: String,
    val aguateroNombre: String,
    val estado: EstadoSolicitudRecarga,
    val litrosSolicitados: Int,
    val litrosReportados: Int? = null,
    val litrosConfirmados: Int? = null,
    val motivo: String,
    val contactoAutorizacion: String? = null,
    val contactoConfirmacion: String? = null,
    val notaOperador: String? = null,
    val distanciaFuenteMetros: Double? = null,
    val solicitadaEn: String,
    val autorizacionExpiraEn: String? = null,
)

data class RecepcionPendiente(
    val rutaPuntoId: Long,
    val rutaId: Long,
    val puntoNombre: String,
    val litrosEsperados: Int,
    val litrosDeclarados: Int,
    val entregadaEn: String,
    val venceEn: String,
)

data class RecepcionAgua(
    val id: String,
    val rutaPuntoId: Long,
    val rutaId: Long,
    val puntoNombre: String,
    val aguateroNombre: String,
    val litrosDeclarados: Int,
    val litrosRecibidos: Int,
    val estado: Int,
    val confirmadoPorNombre: String,
    val comentario: String?,
    val confirmadaEn: String,
)

data class PaginaRecepciones<T>(
    val elementos: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
)

data class IncidenciaOperativa(
    val id: String,
    val jornadaId: Long?,
    val rutaId: Long?,
    val tipo: String,
    val severidad: Int,
    val estado: Int,
    val referencia: String,
    val detalle: String,
    val creadaEn: String,
    val resolucion: String? = null,
)
data class Ruta(
    val id: Long, val fecha: LocalDate, val estado: EstadoRuta,
    val vehiculo: Vehiculo, val paradas: List<Parada>,
)
data class Entrega(
    val id: String, val paradaId: Long, val tipo: TipoEntrega, val ajustaA: String?,
    val litros: Int, val gpsLat: Double?, val gpsLng: Double?, val registradaEn: Long,
)
data class EstadoSyncOperativo(val pendientes: Int = 0, val rechazadas: Int = 0, val ultimoError: String? = null)

data class PuntoAbastecimiento(
    val id: Long, val nombre: String, val lat: Double, val lng: Double,
    val capacidadLitros: Int, val estado: EstadoValidacionPunto,
)
data class PedidoItem(val puntoId: Long, val litros: Int)
enum class PrioridadPedido { NORMAL, URGENTE }
data class CondicionesPedido(
    val prioridad: PrioridadPedido = PrioridadPedido.NORMAL,
    val motivoPrioridad: String? = null,
    val ventanaDesde: Long? = null,
    val ventanaHasta: Long? = null,
)
data class Pedido(
    val id: Long, val items: List<PedidoItem>, val estado: EstadoPedido,
    val creadoEn: Long, val condiciones: CondicionesPedido = CondicionesPedido(),
)
// Vecino (plan 05): no existe tabla — en el backend real vive en Redis.
// Aquí es un value object puro para la UI, siempre en estado "última conocida".
data class PosicionCisterna(
    val lat: Double, val lng: Double, val litrosRestantes: Int,
    val capacidadLitros: Int, val timestampMs: Long,
)
data class CisternaEnZona(
    val rutaId: Long,
    val vehiculoId: Long,
    val capacidadLitros: Int,
    val posicion: PosicionCisterna?,
)
enum class EstadoTracking { EN_VIVO, RESPALDO_REST, SIN_POSICION, ERROR }

// ER §5: el tanque restante es SIEMPRE derivado (ajustes restan con su signo)
fun litrosRestantes(vehiculo: Vehiculo, entregas: List<Entrega>): Int =
    vehiculo.capacidadTanqueLitros - entregas.sumOf { it.litros }

sealed class ErrorDominio(mensaje: String) : Exception(mensaje) {
    class LitrosInvalidos : ErrorDominio("Los litros deben ser mayores a cero")
    class TanqueInsuficiente : ErrorDominio("No queda suficiente agua en el tanque (RN-01)")
    class MotivoRequerido : ErrorDominio("Omitir una parada exige motivo (RA-05)")
    class ComentarioRequerido : ErrorDominio("El motivo OTRO exige comentario de sustento")
    class SinRutaAbierta : ErrorDominio("No hay ruta abierta")
    class PedidoInvalido : ErrorDominio("Los litros deben ser mayores a cero")
    class PedidoNoCancelable : ErrorDominio("Este pedido ya no se puede cancelar")
    class PuntoNoValidable : ErrorDominio("Este punto no está pendiente de aprobación")
}
