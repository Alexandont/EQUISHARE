package com.equishare.app.data

import com.equishare.app.data.local.EquipoEntity
import com.equishare.app.data.local.ReservaEntity
import com.equishare.app.data.remote.dto.CotizacionDto
import com.equishare.app.data.remote.dto.EmpresaDto
import com.equishare.app.data.remote.dto.EquipoDto
import com.equishare.app.data.remote.dto.EquipoEntradaDto
import com.equishare.app.data.remote.dto.ResenaDto
import com.equishare.app.data.remote.dto.ReservaDto
import com.equishare.app.domain.model.Categoria
import com.equishare.app.domain.model.Cotizacion
import com.equishare.app.domain.model.Empresa
import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.model.EquipoEntrada
import com.equishare.app.domain.model.EstadoEquipo
import com.equishare.app.domain.model.EstadoReserva
import com.equishare.app.domain.model.Resena
import com.equishare.app.domain.model.Reserva
import com.equishare.app.domain.model.Rol

/* Conversión entre las tres representaciones: DTO (red), Entity (Room) y modelo de dominio (UI). */

fun EquipoDto.aDominio(): Equipo = Equipo(
    id = id,
    empresaId = empresaId,
    empresaNombre = empresaNombre ?: "Empresa",
    nombre = nombre,
    categoria = Categoria.desde(categoria) ?: Categoria.HERRAMIENTAS,
    descripcion = descripcion,
    precioDia = precioDia,
    fotoUrl = fotoUrl,
    fotoId = fotoId,
    lat = lat,
    lng = lng,
    direccion = direccion,
    estado = EstadoEquipo.desde(estado),
    calificacionPromedio = calificacionPromedio,
    totalResenas = totalResenas,
    distanciaKm = distanciaKm,
)

fun EquipoDto.aEntidad(lista: String, orden: Int, ahora: Long): EquipoEntity = EquipoEntity(
    id = id,
    lista = lista,
    orden = orden,
    empresaId = empresaId,
    empresaNombre = empresaNombre ?: "Empresa",
    nombre = nombre,
    categoria = categoria,
    descripcion = descripcion,
    precioDia = precioDia,
    fotoUrl = fotoUrl,
    fotoId = fotoId,
    lat = lat,
    lng = lng,
    direccion = direccion,
    estado = estado,
    calificacionPromedio = calificacionPromedio,
    totalResenas = totalResenas,
    distanciaKm = distanciaKm,
    guardadoEn = ahora,
)

fun EquipoEntity.aDominio(): Equipo = Equipo(
    id = id,
    empresaId = empresaId,
    empresaNombre = empresaNombre,
    nombre = nombre,
    categoria = Categoria.desde(categoria) ?: Categoria.HERRAMIENTAS,
    descripcion = descripcion,
    precioDia = precioDia,
    fotoUrl = fotoUrl,
    fotoId = fotoId,
    lat = lat,
    lng = lng,
    direccion = direccion,
    estado = EstadoEquipo.desde(estado),
    calificacionPromedio = calificacionPromedio,
    totalResenas = totalResenas,
    distanciaKm = distanciaKm,
)

fun EquipoEntrada.aDto(): EquipoEntradaDto = EquipoEntradaDto(
    nombre = nombre.trim(),
    categoria = categoria.name,
    descripcion = descripcion.trim(),
    precioDia = precioDia,
    lat = lat,
    lng = lng,
    direccion = direccion.trim(),
    fotoId = fotoId,
    visible = visible,
)

fun ReservaDto.aDominio(): Reserva = Reserva(
    id = id,
    equipoId = equipoId,
    equipoNombre = equipoNombre,
    equipoFotoUrl = equipoFotoUrl,
    arrendatarioNombre = arrendatarioNombre,
    arrendadorNombre = arrendadorNombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    dias = dias,
    precioDia = precioDia,
    subtotal = subtotal,
    comision = comision,
    montoArrendador = montoArrendador,
    total = total,
    penalidad = penalidad,
    estado = EstadoReserva.desde(estado),
    rolSolicitante = if (rolSolicitante == Rol.ARRENDADOR.name) Rol.ARRENDADOR else Rol.ARRENDATARIO,
    resenaEnviada = resenaEnviada,
    devueltaEn = devueltaEn,
)

fun ReservaDto.aEntidad(rol: Rol, orden: Int): ReservaEntity = ReservaEntity(
    id = id,
    rol = rol.name,
    orden = orden,
    equipoId = equipoId,
    equipoNombre = equipoNombre,
    equipoFotoUrl = equipoFotoUrl,
    arrendatarioNombre = arrendatarioNombre,
    arrendadorNombre = arrendadorNombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    dias = dias,
    precioDia = precioDia,
    subtotal = subtotal,
    comision = comision,
    montoArrendador = montoArrendador,
    total = total,
    penalidad = penalidad,
    estado = estado,
    resenaEnviada = resenaEnviada,
    devueltaEn = devueltaEn,
)

fun ReservaEntity.aDominio(): Reserva = Reserva(
    id = id,
    equipoId = equipoId,
    equipoNombre = equipoNombre,
    equipoFotoUrl = equipoFotoUrl,
    arrendatarioNombre = arrendatarioNombre,
    arrendadorNombre = arrendadorNombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    dias = dias,
    precioDia = precioDia,
    subtotal = subtotal,
    comision = comision,
    montoArrendador = montoArrendador,
    total = total,
    penalidad = penalidad,
    estado = EstadoReserva.desde(estado),
    rolSolicitante = if (rol == Rol.ARRENDADOR.name) Rol.ARRENDADOR else Rol.ARRENDATARIO,
    resenaEnviada = resenaEnviada,
    devueltaEn = devueltaEn,
)

fun EmpresaDto.aDominio(): Empresa = Empresa(
    id = id,
    razonSocial = razonSocial,
    ruc = ruc,
    telefono = telefono,
    email = email,
    verificada = verificada,
    calificacionComoArrendatario = calificacionComoArrendatario,
    totalResenasComoArrendatario = totalResenasComoArrendatario,
)

fun ResenaDto.aDominio(): Resena = Resena(
    id = id,
    autorNombre = autorNombre,
    estrellas = estrellas,
    comentario = comentario,
    creadoEn = creadoEn,
)

fun CotizacionDto.aDominio(): Cotizacion = Cotizacion(
    dias = dias,
    precioDia = precioDia,
    subtotal = subtotal,
    comision = comision,
    montoArrendador = montoArrendador,
    total = total,
    disponible = disponible,
)
