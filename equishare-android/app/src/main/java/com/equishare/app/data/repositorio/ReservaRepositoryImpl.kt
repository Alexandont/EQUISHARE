package com.equishare.app.data.repositorio

import com.equishare.app.data.aDominio
import com.equishare.app.data.aEntidad
import com.equishare.app.data.local.ReservaDao
import com.equishare.app.data.remote.ApiProvider
import com.equishare.app.data.remote.dto.CambioEstadoDto
import com.equishare.app.data.remote.dto.ResenaEntradaDto
import com.equishare.app.data.remote.dto.SolicitudReservaDto
import com.equishare.app.data.remote.llamarApi
import com.equishare.app.domain.model.Cotizacion
import com.equishare.app.domain.model.EstadoReserva
import com.equishare.app.domain.model.Reserva
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.Rol
import com.equishare.app.domain.repositorio.ReservaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReservaRepositoryImpl(
    private val apiProvider: ApiProvider,
    private val dao: ReservaDao,
) : ReservaRepository {

    override fun observarMias(rol: Rol): Flow<List<Reserva>> =
        dao.observar(rol.name).map { lista -> lista.map { it.aDominio() } }

    override suspend fun refrescarMias(rol: Rol): Resultado<Unit> {
        val parametro = if (rol == Rol.ARRENDADOR) "arrendador" else "arrendatario"
        return when (val resultado = llamarApi { apiProvider.api().misReservas(parametro) }) {
            is Resultado.Exito -> {
                dao.reemplazarRol(rol.name, resultado.datos.items.mapIndexed { i, dto -> dto.aEntidad(rol, i) })
                Resultado.Exito(Unit)
            }
            is Resultado.Error -> resultado
        }
    }

    override suspend fun cotizar(equipoId: String, inicio: String, fin: String): Resultado<Cotizacion> =
        when (val resultado = llamarApi { apiProvider.api().cotizar(SolicitudReservaDto(equipoId, inicio, fin)) }) {
            is Resultado.Exito -> Resultado.Exito(resultado.datos.aDominio())
            is Resultado.Error -> resultado
        }

    override suspend fun crear(equipoId: String, inicio: String, fin: String): Resultado<Reserva> {
        val resultado = llamarApi { apiProvider.api().crearReserva(SolicitudReservaDto(equipoId, inicio, fin)) }
        if (resultado is Resultado.Exito) refrescarMias(Rol.ARRENDATARIO)
        return when (resultado) {
            is Resultado.Exito -> Resultado.Exito(resultado.datos.aDominio())
            is Resultado.Error -> resultado
        }
    }

    override suspend fun cancelar(id: String): Resultado<Reserva> =
        actualizarYRefrescar(llamarApi { apiProvider.api().cancelarReserva(id) })

    override suspend fun cambiarEstado(id: String, estado: EstadoReserva): Resultado<Reserva> =
        actualizarYRefrescar(llamarApi { apiProvider.api().cambiarEstado(id, CambioEstadoDto(estado.name)) })

    override suspend fun calificar(id: String, estrellas: Int, comentario: String): Resultado<Unit> {
        val resultado = llamarApi { apiProvider.api().calificar(id, ResenaEntradaDto(estrellas, comentario.trim())) }
        return when (resultado) {
            is Resultado.Exito -> {
                refrescarMias(Rol.ARRENDATARIO)
                refrescarMias(Rol.ARRENDADOR)
                Resultado.Exito(Unit)
            }
            is Resultado.Error -> resultado
        }
    }

    private suspend fun actualizarYRefrescar(
        resultado: Resultado<com.equishare.app.data.remote.dto.ReservaDto>,
    ): Resultado<Reserva> = when (resultado) {
        is Resultado.Exito -> {
            refrescarMias(Rol.ARRENDATARIO)
            refrescarMias(Rol.ARRENDADOR)
            Resultado.Exito(resultado.datos.aDominio())
        }
        is Resultado.Error -> resultado
    }
}
