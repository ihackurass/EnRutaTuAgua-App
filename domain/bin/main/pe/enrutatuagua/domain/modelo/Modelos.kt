package pe.enrutatuagua.domain.modelo

import java.time.LocalDate

// Códigos idénticos a sql/01-esquema.sql (catalogo_valor)
enum class Rol(val codigo: Int, val etiqueta: String) {
    AGUATERO(1, "Aguatero (cisterna)"), VECINO(2, "Vecino"),
    PRESIDENTE(3, "Presidente de asociación"), ADMIN(4, "Administrador")
}
enum class EstadoRuta(val codigo: Int) { ABIERTA(1), CERRADA(2) }
enum class EstadoEntrega(val codigo: Int) { PENDIENTE(1), ENTREGADO(2), OMITIDO(3) }
enum class MotivoOmision(val codigo: Int, val etiqueta: String) {
    CALLE_CERRADA(1, "Calle cerrada"), NADIE_PRESENTE(2, "Nadie presente"),
    FALTA_AGUA(3, "Falta de agua"), OTRO(4, "Otro")
}
enum class TipoEntrega(val codigo: Int) { ENTREGA(1), AJUSTE(2) }

data class Vehiculo(val id: Long, val placa: String, val capacidadTanqueLitros: Int)
data class Punto(val id: Long, val nombre: String, val lat: Double, val lng: Double, val capacidadLitros: Int)
data class Parada(
    val id: Long, val punto: Punto, val orden: Int, val capacidadEsperadaLitros: Int,
    val estadoEntrega: EstadoEntrega, val motivoOmision: MotivoOmision?, val comentarioOmision: String?,
)
data class Ruta(
    val id: Long, val fecha: LocalDate, val estado: EstadoRuta,
    val vehiculo: Vehiculo, val paradas: List<Parada>,
)
data class Entrega(
    val id: String, val paradaId: Long, val tipo: TipoEntrega, val ajustaA: String?,
    val litros: Int, val gpsLat: Double?, val gpsLng: Double?, val registradaEn: Long,
)

// ER §5: el tanque restante es SIEMPRE derivado (ajustes restan con su signo)
fun litrosRestantes(vehiculo: Vehiculo, entregas: List<Entrega>): Int =
    vehiculo.capacidadTanqueLitros - entregas.sumOf { it.litros }

sealed class ErrorDominio(mensaje: String) : Exception(mensaje) {
    class LitrosInvalidos : ErrorDominio("Los litros deben ser mayores a cero")
    class TanqueInsuficiente : ErrorDominio("No queda suficiente agua en el tanque (RN-01)")
    class MotivoRequerido : ErrorDominio("Omitir una parada exige motivo (RA-05)")
    class ComentarioRequerido : ErrorDominio("El motivo OTRO exige comentario de sustento")
    class SinRutaAbierta : ErrorDominio("No hay ruta abierta")
}
