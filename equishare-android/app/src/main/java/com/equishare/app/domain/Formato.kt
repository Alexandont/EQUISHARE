package com.equishare.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatos de presentación (moneda en soles y fechas en español). */
object Formato {
    private val LOCALE_PE: Locale = Locale.forLanguageTag("es-PE")
    private val FECHA_CORTA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_PE)

    fun soles(monto: Double): String = "S/ " + String.format(LOCALE_PE, "%,.2f", monto)

    fun fecha(iso: String): String = runCatching { LocalDate.parse(iso).format(FECHA_CORTA) }.getOrDefault(iso)

    fun rango(inicio: String, fin: String): String =
        if (inicio == fin) fecha(inicio) else "${fecha(inicio)} al ${fecha(fin)}"

    fun calificacion(promedio: Double, total: Int): String =
        if (total == 0) "Sin calificaciones" else String.format(LOCALE_PE, "%.1f / 5 (%d)", promedio, total)

    fun distancia(km: Double?): String? = km?.let {
        if (it < 1.0) "${(it * 1000).toInt()} m" else String.format(LOCALE_PE, "%.1f km", it)
    }
}
