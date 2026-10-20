package com.equishare.app.domain.reglas

import com.equishare.app.domain.model.Cotizacion
import com.equishare.app.domain.model.RangoOcupado
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Reglas de negocio de EQUISHARE replicadas en el cliente para validar antes de llamar a la API
 * y mostrar al usuario el desglose de costos. El servidor vuelve a validarlas (fuente de verdad).
 */
object ReglasNegocio {
    const val COMISION_PLATAFORMA = 0.12
    const val PENALIDAD_CANCELACION = 0.20
    const val ANTICIPACION_MINIMA_HORAS = 24L
    const val LIMITE_CANCELACION_HORAS = 12L
    const val VENTANA_RESENA_HORAS = 48L
    const val MAX_DIAS_RESERVA = 90

    val ZONA_LIMA: ZoneOffset = ZoneOffset.ofHours(-5)
    val HORA_ENTREGA: LocalTime = LocalTime.of(8, 0)

    fun redondear2(valor: Double): Double = (valor * 100.0).roundToLong() / 100.0

    /** Momento de entrega del equipo: 08:00 (hora de Lima) del día de inicio. */
    fun instanteInicio(fechaInicio: LocalDate): Instant =
        fechaInicio.atTime(HORA_ENTREGA).toInstant(ZONA_LIMA)

    fun hoyEnLima(ahora: Instant): LocalDate = ahora.atOffset(ZONA_LIMA).toLocalDate()

    /** Primer día que se puede elegir como inicio cumpliendo la anticipación mínima. */
    fun primerDiaReservable(ahora: Instant): LocalDate {
        var dia = hoyEnLima(ahora)
        while (!cumpleAnticipacion(dia, ahora)) dia = dia.plusDays(1)
        return dia
    }

    fun diasAlquiler(inicio: LocalDate, fin: LocalDate): Int =
        (ChronoUnit.DAYS.between(inicio, fin) + 1).toInt()

    /** El arrendatario paga el subtotal; la comisión del 12 % se descuenta al arrendador. */
    fun cotizar(precioDia: Double, inicio: LocalDate, fin: LocalDate, disponible: Boolean = true): Cotizacion {
        val dias = diasAlquiler(inicio, fin)
        val subtotal = redondear2(precioDia * dias)
        val comision = redondear2(subtotal * COMISION_PLATAFORMA)
        return Cotizacion(
            dias = dias,
            precioDia = redondear2(precioDia),
            subtotal = subtotal,
            comision = comision,
            montoArrendador = redondear2(subtotal - comision),
            total = subtotal,
            disponible = disponible,
        )
    }

    fun cumpleAnticipacion(inicio: LocalDate, ahora: Instant): Boolean =
        Duration.between(ahora, instanteInicio(inicio)).toMinutes() >= ANTICIPACION_MINIMA_HORAS * 60

    /** Devuelve el mensaje de error del rango elegido o null si es válido. */
    fun validarRango(inicio: LocalDate, fin: LocalDate, ahora: Instant): String? {
        if (fin.isBefore(inicio)) return "La fecha de fin no puede ser anterior a la de inicio."
        if (diasAlquiler(inicio, fin) > MAX_DIAS_RESERVA) return "Una reserva no puede superar $MAX_DIAS_RESERVA días."
        if (!cumpleAnticipacion(inicio, ahora)) {
            return "Las reservas se hacen con al menos $ANTICIPACION_MINIMA_HORAS horas de anticipación (entrega a las 08:00)."
        }
        return null
    }

    /** Dos rangos (días incluidos) se cruzan si a.inicio <= b.fin y b.inicio <= a.fin. */
    fun haySolapamiento(ocupados: List<RangoOcupado>, inicio: LocalDate, fin: LocalDate): Boolean =
        ocupados.any { rango ->
            val a = LocalDate.parse(rango.fechaInicio)
            val b = LocalDate.parse(rango.fechaFin)
            !a.isAfter(fin) && !inicio.isAfter(b)
        }

    /** Penalidad que se aplicaría si se cancela en este momento. */
    fun penalidadSiCancela(fechaInicio: LocalDate, total: Double, ahora: Instant): Double {
        val minutos = Duration.between(ahora, instanteInicio(fechaInicio)).toMinutes()
        return if (minutos < LIMITE_CANCELACION_HORAS * 60) redondear2(total * PENALIDAD_CANCELACION) else 0.0
    }

    /** Se puede calificar dentro de las 48 horas posteriores a la devolución. */
    fun dentroDeVentanaResena(devueltaEn: Instant?, ahora: Instant): Boolean {
        if (devueltaEn == null) return false
        val horas = Duration.between(devueltaEn, ahora).toMinutes() / 60.0
        return horas in 0.0..VENTANA_RESENA_HORAS.toDouble()
    }

    /** Distancia aproximada en km entre dos coordenadas (haversine). */
    fun distanciaKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return redondear2(2 * r * asin(sqrt(a)))
    }
}
