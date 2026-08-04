package pe.enrutatuagua.data.red

import pe.enrutatuagua.domain.modelo.*
import java.time.Instant
import java.time.LocalDate

fun VehiculoDto.aDominio() = Vehiculo(id, placa, capacidadTanqueLitros)

fun PuntoDto.aDominio() = Punto(id, nombre, lat, lng, capacidadLitros)

/**
 * Auditoría rol Vecino: `estado` viaja nullable en `PuntoDto` (compartido con
 * `Punto.aDominio()` de arriba, que no lo necesita) pero `/puntos/zona`
 * siempre lo llena -- si llegara nulo acá sería un contrato roto del
 * backend, no un caso de negocio a tolerar en silencio.
 */
fun PuntoDto.aPuntoAbastecimiento() = PuntoAbastecimiento(
    id = id, nombre = nombre, lat = lat, lng = lng, capacidadLitros = capacidadLitros,
    estado = EstadoValidacionPunto.valueOf(checkNotNull(estado) { "estado nulo en /puntos/zona" }),
)

/**
 * Auditoría rol Vecino (deuda conocida, mismo espíritu que
 * `entregasAproximadas()` de abajo): tracking no expone litros reales de la
 * cisterna en tránsito (vive en `entregas`, sin endpoint scoped a una ruta
 * ajena) -- se aproxima a "tanque lleno" con la capacidad real del
 * vehículo. Sólo afecta la cifra mostrada al vecino, no ningún dato que el
 * backend valide.
 */
fun PosicionVivaDto.aPosicionCisterna(capacidadTanqueLitros: Int) = PosicionCisterna(
    lat = lat, lng = lng,
    litrosRestantes = capacidadTanqueLitros, capacidadLitros = capacidadTanqueLitros,
    timestampMs = Instant.parse(registradoEn).toEpochMilli(),
)

fun ParadaDto.aDominio(punto: Punto) = Parada(
    id = id,
    punto = punto,
    orden = orden,
    capacidadEsperadaLitros = capacidadEsperadaLitros,
    estadoEntrega = EstadoEntrega.valueOf(estadoEntrega),
    motivoOmision = motivoOmision?.let { MotivoOmision.valueOf(it) },
    comentarioOmision = comentarioOmision,
)

fun RutaDto.aDominio(vehiculo: Vehiculo, puntos: Map<Long, Punto>) = Ruta(
    id = id,
    fecha = LocalDate.parse(fecha),
    estado = EstadoRuta.valueOf(estado),
    vehiculo = vehiculo,
    paradas = paradas.map { it.aDominio(puntos.getValue(it.puntoId)) },
)

/**
 * ANDROID-PLAN-02 (deuda conocida, T4): el backend no expone todavía el
 * litraje REAL entregado por parada (vive en el módulo `entregas`, sin
 * endpoint scoped a una ruta) -- se aproxima con `capacidadEsperadaLitros`
 * para paradas ENTREGADO (0 para el resto). El backend valida la capacidad
 * del tanque de verdad server-side (`ServicioEntregas`), así que esta
 * aproximación sólo afecta la cifra que se MUESTRA en pantalla mientras el
 * aguatero está en ruta, no la integridad de los datos.
 */
fun Ruta.entregasAproximadas(): List<Entrega> = paradas
    .filter { it.estadoEntrega == EstadoEntrega.ENTREGADO }
    .map { parada ->
        Entrega(
            id = "aprox-${parada.id}", paradaId = parada.id, tipo = TipoEntrega.ENTREGA, ajustaA = null,
            litros = parada.capacidadEsperadaLitros, gpsLat = null, gpsLng = null, registradaEn = 0L,
        )
    }
