package com.equishare.app.data.remote.dto

import com.google.gson.JsonElement

/*
 * Objetos de transferencia (contrato JSON de la API, ver equishare-api/docs/openapi.yaml).
 * Todos los campos tienen valor por defecto para que Gson no falle si el servidor omite alguno.
 */

data class SaludDto(
    val estado: String = "",
    val servicio: String = "",
    val modoAutenticacion: String = "",
)

data class PaginaDto<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val size: Int = 0,
    val total: Int = 0,
)

data class EquipoDto(
    val id: String = "",
    val empresaId: String = "",
    val empresaNombre: String? = null,
    val nombre: String = "",
    val categoria: String = "",
    val descripcion: String = "",
    val precioDia: Double = 0.0,
    val fotoUrl: String? = null,
    val fotoId: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val direccion: String = "",
    val estado: String = "",
    val calificacionPromedio: Double = 0.0,
    val totalResenas: Int = 0,
    val distanciaKm: Double? = null,
)

data class EquipoEntradaDto(
    val nombre: String,
    val categoria: String,
    val descripcion: String,
    val precioDia: Double,
    val lat: Double,
    val lng: Double,
    val direccion: String,
    val fotoId: String?,
    val visible: Boolean,
)

data class FotoDto(
    val id: String = "",
    val url: String = "",
    val aprobada: Boolean = false,
    val motivos: List<String> = emptyList(),
)

data class RangoDto(val fechaInicio: String = "", val fechaFin: String = "")

data class DisponibilidadDto(val equipoId: String = "", val ocupados: List<RangoDto> = emptyList())

data class ResenaDto(
    val id: String = "",
    val autorNombre: String = "",
    val estrellas: Int = 0,
    val comentario: String = "",
    val creadoEn: String = "",
)

data class ResenasDto(
    val items: List<ResenaDto> = emptyList(),
    val promedio: Double = 0.0,
    val total: Int = 0,
)

data class EmpresaDto(
    val id: String = "",
    val razonSocial: String = "",
    val ruc: String = "",
    val telefono: String = "",
    val email: String? = null,
    val verificada: Boolean = false,
    val calificacionComoArrendatario: Double = 0.0,
    val totalResenasComoArrendatario: Int = 0,
)

data class EmpresaEntradaDto(val ruc: String, val razonSocial: String, val telefono: String)

data class SolicitudReservaDto(val equipoId: String, val fechaInicio: String, val fechaFin: String)

data class CotizacionDto(
    val dias: Int = 0,
    val precioDia: Double = 0.0,
    val subtotal: Double = 0.0,
    val comision: Double = 0.0,
    val montoArrendador: Double = 0.0,
    val total: Double = 0.0,
    val disponible: Boolean = false,
)

data class ReservaDto(
    val id: String = "",
    val equipoId: String = "",
    val equipoNombre: String = "",
    val equipoFotoUrl: String? = null,
    val arrendatarioId: String = "",
    val arrendatarioNombre: String = "",
    val arrendadorId: String = "",
    val arrendadorNombre: String = "",
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val dias: Int = 0,
    val precioDia: Double = 0.0,
    val subtotal: Double = 0.0,
    val comision: Double = 0.0,
    val montoArrendador: Double = 0.0,
    val total: Double = 0.0,
    val penalidad: Double = 0.0,
    val estado: String = "",
    val canceladaPor: String? = null,
    val devueltaEn: String? = null,
    val creadoEn: String = "",
    val rolSolicitante: String = "",
    val resenaEnviada: Boolean = false,
)

data class CambioEstadoDto(val estado: String)

data class ResenaEntradaDto(val estrellas: Int, val comentario: String)

data class ResenaCreadaDto(val id: String = "", val estrellas: Int = 0, val comentario: String = "")

/** Cuerpo de error uniforme: { "error": { "codigo", "mensaje", "detalles" } } */
data class ErrorRespuestaDto(val error: ErrorCuerpoDto? = null)

data class ErrorCuerpoDto(
    val codigo: String? = null,
    val mensaje: String? = null,
    val detalles: List<JsonElement>? = null,
)
