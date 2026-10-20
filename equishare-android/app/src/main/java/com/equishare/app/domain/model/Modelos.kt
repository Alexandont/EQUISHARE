package com.equishare.app.domain.model

/** Categorías de equipos que maneja la plataforma (mismos valores que la API). */
enum class Categoria(val etiqueta: String) {
    CONSTRUCCION("Construcción"),
    EVENTOS("Eventos"),
    HERRAMIENTAS("Herramientas"),
    TECNOLOGIA("Tecnología");

    companion object {
        fun desde(valor: String?): Categoria? = entries.firstOrNull { it.name == valor }
    }
}

enum class EstadoEquipo(val etiqueta: String) {
    PUBLICADO("Publicado"),
    OCULTO("Oculto"),
    PENDIENTE_FOTO("Falta foto aprobada"),
    EN_REVISION("En revisión");

    companion object {
        fun desde(valor: String?): EstadoEquipo = entries.firstOrNull { it.name == valor } ?: OCULTO
    }
}

data class Equipo(
    val id: String,
    val empresaId: String,
    val empresaNombre: String,
    val nombre: String,
    val categoria: Categoria,
    val descripcion: String,
    val precioDia: Double,
    val fotoUrl: String?,
    val fotoId: String?,
    val lat: Double,
    val lng: Double,
    val direccion: String,
    val estado: EstadoEquipo,
    val calificacionPromedio: Double,
    val totalResenas: Int,
    val distanciaKm: Double?,
)

enum class EstadoReserva(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    CONFIRMADA("Confirmada"),
    EN_CURSO("En curso"),
    DEVUELTA("Devuelta"),
    CANCELADA("Cancelada");

    /** Siguiente estado que puede asignar el arrendador, o null si no hay. */
    fun siguienteParaArrendador(): EstadoReserva? = when (this) {
        PENDIENTE -> CONFIRMADA
        CONFIRMADA -> EN_CURSO
        EN_CURSO -> DEVUELTA
        DEVUELTA, CANCELADA -> null
    }

    val esCancelable: Boolean get() = this == PENDIENTE || this == CONFIRMADA

    companion object {
        fun desde(valor: String?): EstadoReserva = entries.firstOrNull { it.name == valor } ?: PENDIENTE
    }
}

enum class Rol { ARRENDATARIO, ARRENDADOR }

data class Reserva(
    val id: String,
    val equipoId: String,
    val equipoNombre: String,
    val equipoFotoUrl: String?,
    val arrendatarioNombre: String,
    val arrendadorNombre: String,
    val fechaInicio: String,
    val fechaFin: String,
    val dias: Int,
    val precioDia: Double,
    val subtotal: Double,
    val comision: Double,
    val montoArrendador: Double,
    val total: Double,
    val penalidad: Double,
    val estado: EstadoReserva,
    val rolSolicitante: Rol,
    val resenaEnviada: Boolean,
    val devueltaEn: String?,
)

data class Empresa(
    val id: String,
    val razonSocial: String,
    val ruc: String,
    val telefono: String,
    val email: String?,
    val verificada: Boolean,
    val calificacionComoArrendatario: Double,
    val totalResenasComoArrendatario: Int,
)

data class Resena(
    val id: String,
    val autorNombre: String,
    val estrellas: Int,
    val comentario: String,
    val creadoEn: String,
)

data class Cotizacion(
    val dias: Int,
    val precioDia: Double,
    val subtotal: Double,
    val comision: Double,
    val montoArrendador: Double,
    val total: Double,
    val disponible: Boolean,
)

data class RangoOcupado(val fechaInicio: String, val fechaFin: String)

data class Sesion(val uid: String, val email: String?, val esDemo: Boolean)

/** Datos del formulario de publicación/edición de un equipo. */
data class EquipoEntrada(
    val nombre: String,
    val categoria: Categoria,
    val descripcion: String,
    val precioDia: Double,
    val lat: Double,
    val lng: Double,
    val direccion: String,
    val fotoId: String?,
    val visible: Boolean,
)

data class FotoSubida(val id: String, val url: String)

data class Coordenadas(val lat: Double, val lng: Double)
