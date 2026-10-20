package com.equishare.app.domain.model

/** Tipos de error que la interfaz sabe presentar al usuario. */
enum class TipoError {
    SIN_CONEXION,
    TIEMPO_AGOTADO,
    NO_AUTENTICADO,
    PROHIBIDO,
    NO_ENCONTRADO,
    CONFLICTO,
    VALIDACION,
    REGLA_NEGOCIO,
    SERVIDOR,
    DESCONOCIDO;

    companion object {
        /** Traduce un código HTTP de la API a un tipo de error de la app. */
        fun desdeCodigoHttp(codigo: Int): TipoError = when (codigo) {
            400, 413, 415 -> VALIDACION
            401 -> NO_AUTENTICADO
            403 -> PROHIBIDO
            404 -> NO_ENCONTRADO
            409 -> CONFLICTO
            422 -> REGLA_NEGOCIO
            in 500..599 -> SERVIDOR
            else -> DESCONOCIDO
        }

        fun mensajePorDefecto(tipo: TipoError): String = when (tipo) {
            SIN_CONEXION -> "Sin conexión con el servidor. Revisa tu internet o la dirección del servidor."
            TIEMPO_AGOTADO -> "El servidor tardó demasiado en responder. Inténtalo de nuevo."
            NO_AUTENTICADO -> "Tu sesión expiró. Vuelve a iniciar sesión."
            PROHIBIDO -> "No tienes permiso para realizar esta acción."
            NO_ENCONTRADO -> "El recurso solicitado no existe."
            CONFLICTO -> "La operación entra en conflicto con datos existentes."
            VALIDACION -> "Revisa los datos ingresados."
            REGLA_NEGOCIO -> "La operación no cumple una regla del negocio."
            SERVIDOR -> "Ocurrió un error en el servidor. Inténtalo más tarde."
            DESCONOCIDO -> "Ocurrió un error inesperado."
        }
    }
}

data class ErrorApp(
    val tipo: TipoError,
    val mensaje: String = TipoError.mensajePorDefecto(tipo),
    val codigo: String? = null,
    val detalles: List<String> = emptyList(),
)

/** Resultado de una operación: éxito con datos o error controlado (nunca una excepción en la UI). */
sealed interface Resultado<out T> {
    data class Exito<T>(val datos: T) : Resultado<T>
    data class Error(val error: ErrorApp) : Resultado<Nothing>
}

inline fun <T, R> Resultado<T>.mapear(transformar: (T) -> R): Resultado<R> = when (this) {
    is Resultado.Exito -> Resultado.Exito(transformar(datos))
    is Resultado.Error -> this
}

inline fun <T> Resultado<T>.alExito(accion: (T) -> Unit): Resultado<T> {
    if (this is Resultado.Exito) accion(datos)
    return this
}

fun <T> Resultado<T>.datosONull(): T? = (this as? Resultado.Exito)?.datos

fun <T> Resultado<T>.errorONull(): ErrorApp? = (this as? Resultado.Error)?.error
