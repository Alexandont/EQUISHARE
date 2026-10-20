package com.equishare.app.data.repositorio

import com.equishare.app.data.aDominio
import com.equishare.app.data.aDto
import com.equishare.app.data.aEntidad
import com.equishare.app.data.local.EquipoDao
import com.equishare.app.data.remote.ApiProvider
import com.equishare.app.data.remote.llamarApi
import com.equishare.app.data.sistema.LectorImagen
import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.model.EquipoEntrada
import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.FotoSubida
import com.equishare.app.domain.model.RangoOcupado
import com.equishare.app.domain.model.Resena
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.TipoError
import com.equishare.app.domain.repositorio.EquipoRepository
import com.equishare.app.domain.repositorio.FiltroCatalogo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Repositorio "offline-first": la UI siempre lee de Room; la red solo actualiza la caché.
 */
class EquipoRepositoryImpl(
    private val apiProvider: ApiProvider,
    private val dao: EquipoDao,
    private val lectorImagen: LectorImagen,
) : EquipoRepository {

    override fun observarCatalogo(): Flow<List<Equipo>> =
        dao.observar(LISTA_CATALOGO).map { lista -> lista.map { it.aDominio() } }

    override suspend fun refrescarCatalogo(filtro: FiltroCatalogo): Resultado<Unit> {
        val resultado = llamarApi {
            apiProvider.api().listarEquipos(
                q = filtro.texto.trim().ifBlank { null },
                categoria = filtro.categoria?.name,
                lat = filtro.ubicacion?.lat,
                lng = filtro.ubicacion?.lng,
                radioKm = filtro.radioKm,
            )
        }
        return when (resultado) {
            is Resultado.Exito -> {
                val ahora = System.currentTimeMillis()
                dao.reemplazarLista(
                    LISTA_CATALOGO,
                    resultado.datos.items.mapIndexed { i, dto -> dto.aEntidad(LISTA_CATALOGO, i, ahora) },
                )
                Resultado.Exito(Unit)
            }
            is Resultado.Error -> resultado
        }
    }

    override fun observarPropios(): Flow<List<Equipo>> =
        dao.observar(LISTA_PROPIOS).map { lista -> lista.map { it.aDominio() } }

    override suspend fun refrescarPropios(): Resultado<Unit> {
        return when (val resultado = llamarApi { apiProvider.api().misEquipos() }) {
            is Resultado.Exito -> {
                val ahora = System.currentTimeMillis()
                dao.reemplazarLista(
                    LISTA_PROPIOS,
                    resultado.datos.items.mapIndexed { i, dto -> dto.aEntidad(LISTA_PROPIOS, i, ahora) },
                )
                Resultado.Exito(Unit)
            }
            is Resultado.Error -> resultado
        }
    }

    override suspend fun obtener(id: String): Resultado<Equipo> {
        return when (val resultado = llamarApi { apiProvider.api().obtenerEquipo(id) }) {
            is Resultado.Exito -> Resultado.Exito(resultado.datos.aDominio())
            is Resultado.Error -> {
                // Sin conexión: se muestra la copia guardada si existe.
                val local = if (resultado.error.tipo == TipoError.SIN_CONEXION ||
                    resultado.error.tipo == TipoError.TIEMPO_AGOTADO
                ) dao.buscar(id) else null
                if (local != null) Resultado.Exito(local.aDominio()) else resultado
            }
        }
    }

    override suspend fun crear(entrada: EquipoEntrada): Resultado<Equipo> {
        val resultado = llamarApi { apiProvider.api().crearEquipo(entrada.aDto()) }
        if (resultado is Resultado.Exito) refrescarPropios()
        return when (resultado) {
            is Resultado.Exito -> Resultado.Exito(resultado.datos.aDominio())
            is Resultado.Error -> resultado
        }
    }

    override suspend fun actualizar(id: String, entrada: EquipoEntrada): Resultado<Equipo> {
        val resultado = llamarApi { apiProvider.api().actualizarEquipo(id, entrada.aDto()) }
        if (resultado is Resultado.Exito) refrescarPropios()
        return when (resultado) {
            is Resultado.Exito -> Resultado.Exito(resultado.datos.aDominio())
            is Resultado.Error -> resultado
        }
    }

    override suspend fun eliminar(id: String): Resultado<Unit> {
        val resultado = llamarApi {
            val respuesta = apiProvider.api().eliminarEquipo(id)
            if (!respuesta.isSuccessful) throw retrofit2.HttpException(respuesta)
        }
        if (resultado is Resultado.Exito) dao.eliminar(id)
        return resultado
    }

    override suspend fun subirFoto(uri: String): Resultado<FotoSubida> {
        val imagen = lectorImagen.preparar(uri)
            ?: return Resultado.Error(ErrorApp(TipoError.VALIDACION, "No se pudo leer la imagen seleccionada."))
        val parte = MultipartBody.Part.createFormData(
            "foto",
            "foto.jpg",
            imagen.bytes.toRequestBody("image/jpeg".toMediaType()),
        )
        return when (val resultado = llamarApi { apiProvider.api().subirFoto(parte) }) {
            is Resultado.Exito -> Resultado.Exito(FotoSubida(resultado.datos.id, resultado.datos.url))
            is Resultado.Error -> resultado
        }
    }

    override suspend fun disponibilidad(id: String): Resultado<List<RangoOcupado>> =
        when (val resultado = llamarApi { apiProvider.api().disponibilidad(id) }) {
            is Resultado.Exito -> Resultado.Exito(resultado.datos.ocupados.map { RangoOcupado(it.fechaInicio, it.fechaFin) })
            is Resultado.Error -> resultado
        }

    override suspend fun resenas(id: String): Resultado<List<Resena>> =
        when (val resultado = llamarApi { apiProvider.api().resenasEquipo(id) }) {
            is Resultado.Exito -> Resultado.Exito(resultado.datos.items.map { it.aDominio() })
            is Resultado.Error -> resultado
        }

    companion object {
        const val LISTA_CATALOGO = "CATALOGO"
        const val LISTA_PROPIOS = "PROPIOS"
    }
}
