package com.equishare.app.data.remote

import com.equishare.app.data.remote.dto.ErrorRespuestaDto
import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.TipoError
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

private val gson = Gson()

/**
 * Ejecuta una llamada a la API y convierte cualquier fallo en un [Resultado.Error] con un mensaje
 * entendible: errores HTTP (400, 401, 403, 404, 409, 422, 5xx), falta de conexión y tiempo agotado.
 */
suspend fun <T> llamarApi(bloque: suspend () -> T): Resultado<T> = try {
    Resultado.Exito(bloque())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    Resultado.Error(errorDesdeHttp(e))
} catch (e: SocketTimeoutException) {
    Resultado.Error(ErrorApp(TipoError.TIEMPO_AGOTADO))
} catch (e: IOException) {
    Resultado.Error(ErrorApp(TipoError.SIN_CONEXION))
} catch (e: IllegalArgumentException) {
    Resultado.Error(ErrorApp(TipoError.DESCONOCIDO, "La dirección del servidor no es válida. Revísala en Perfil > Conexión con el servidor."))
} catch (e: Exception) {
    Resultado.Error(ErrorApp(TipoError.DESCONOCIDO, e.message ?: TipoError.mensajePorDefecto(TipoError.DESCONOCIDO)))
}

fun errorDesdeHttp(e: HttpException): ErrorApp {
    val tipo = TipoError.desdeCodigoHttp(e.code())
    val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val dto = runCatching { gson.fromJson(cuerpo, ErrorRespuestaDto::class.java) }.getOrNull()?.error
    val detalles = dto?.detalles.orEmpty().mapNotNull { elemento ->
        when {
            elemento.isJsonPrimitive -> elemento.asString
            elemento.isJsonObject && elemento.asJsonObject.has("mensaje") -> elemento.asJsonObject.get("mensaje").asString
            else -> null
        }
    }
    return ErrorApp(
        tipo = tipo,
        mensaje = dto?.mensaje?.takeIf { it.isNotBlank() } ?: TipoError.mensajePorDefecto(tipo),
        codigo = dto?.codigo,
        detalles = detalles,
    )
}
