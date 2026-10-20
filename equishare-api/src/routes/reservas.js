import { Router } from 'express';
import crypto from 'node:crypto';
import { errores } from '../errors.js';
import {
  validarFechasReserva,
  haySolapamiento,
  cotizar,
  calcularPenalidad,
  verificarVentanaResena,
  requiereRevision,
  promedio,
  TRANSICIONES_ARRENDADOR,
} from '../domain/reglas.js';
import { validarResena } from '../domain/validaciones.js';
import { requiereEmpresaVerificada } from './empresas.js';
import { reservaDto } from './mapeo.js';

export function rutasReservas({ store, autenticar, reloj }) {
  const r = Router();
  const verificada = requiereEmpresaVerificada(store);

  function reservaParticipante(req) {
    const reserva = store.obtener('reservas', req.params.id);
    if (!reserva) throw errores.noEncontrado('La reserva');
    const uid = req.usuario.uid;
    if (reserva.arrendatarioId !== uid && reserva.arrendadorId !== uid) {
      throw errores.prohibido('Solo las empresas que participan en la reserva pueden verla o modificarla.');
    }
    return reserva;
  }

  // POST /api/reservas/cotizacion -> cálculo transparente antes de reservar (sin crear nada)
  r.post('/cotizacion', autenticar, (req, res) => {
    const { equipoId, fechaInicio, fechaFin } = req.body ?? {};
    const equipo = store.obtener('equipos', equipoId);
    if (!equipo || equipo.estado !== 'PUBLICADO') throw errores.noEncontrado('El equipo');
    const lista = validarFechasReserva(fechaInicio, fechaFin, reloj());
    if (lista.length) throw errores.reglaNegocio('FECHAS_NO_VALIDAS', lista[0], lista);
    const disponible = !haySolapamiento(store.listar('reservas', (x) => x.equipoId === equipo.id), fechaInicio, fechaFin);
    res.json({ equipoId, fechaInicio, fechaFin, disponible, ...cotizar(equipo.precioDia, fechaInicio, fechaFin) });
  });

  // POST /api/reservas -> crear (RN-01, RN-02, RN-03, RN-07)
  r.post('/', autenticar, verificada, (req, res) => {
    const { equipoId, fechaInicio, fechaFin } = req.body ?? {};
    if (typeof equipoId !== 'string' || !equipoId) {
      throw errores.validacion([{ campo: 'equipoId', mensaje: 'Indica el equipo a reservar.' }]);
    }
    const equipo = store.obtener('equipos', equipoId);
    if (!equipo || equipo.estado !== 'PUBLICADO') throw errores.noEncontrado('El equipo');
    if (equipo.empresaId === req.usuario.uid) {
      throw errores.reglaNegocio('RESERVA_PROPIA', 'No puedes reservar un equipo de tu propia empresa.');
    }
    const lista = validarFechasReserva(fechaInicio, fechaFin, reloj());
    if (lista.length) throw errores.reglaNegocio('FECHAS_NO_VALIDAS', lista[0], lista);
    const delEquipo = store.listar('reservas', (x) => x.equipoId === equipo.id);
    if (haySolapamiento(delEquipo, fechaInicio, fechaFin)) {
      throw errores.conflicto('RESERVA_SOLAPADA', 'El equipo ya tiene una reserva activa en esas fechas. Elige otro rango.');
    }
    const ahora = reloj().toISOString();
    const reserva = store.insertar('reservas', {
      id: crypto.randomUUID(),
      equipoId: equipo.id,
      equipoNombre: equipo.nombre,
      arrendatarioId: req.usuario.uid,
      arrendadorId: equipo.empresaId,
      fechaInicio,
      fechaFin,
      ...cotizar(equipo.precioDia, fechaInicio, fechaFin),
      penalidad: 0,
      estado: 'PENDIENTE',
      creadoEn: ahora,
      actualizadoEn: ahora,
    });
    res.status(201).location(`/api/reservas/${reserva.id}`).json(reservaDto(req, store, reserva, req.usuario.uid));
  });

  // GET /api/reservas/mias?rol=arrendatario|arrendador
  r.get('/mias', autenticar, (req, res) => {
    const rol = String(req.query.rol ?? 'arrendatario');
    if (!['arrendatario', 'arrendador'].includes(rol)) {
      throw errores.validacion([{ campo: 'rol', mensaje: 'rol debe ser arrendatario o arrendador.' }]);
    }
    const campo = rol === 'arrendador' ? 'arrendadorId' : 'arrendatarioId';
    const items = store
      .listar('reservas', (x) => x[campo] === req.usuario.uid)
      .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn))
      .map((x) => reservaDto(req, store, x, req.usuario.uid));
    res.json({ items, page: 1, size: items.length, total: items.length });
  });

  r.get('/:id', autenticar, (req, res) => {
    res.json(reservaDto(req, store, reservaParticipante(req), req.usuario.uid));
  });

  // PATCH /api/reservas/:id/cancelar (RN-08)
  r.patch('/:id/cancelar', autenticar, (req, res) => {
    const reserva = reservaParticipante(req);
    if (!['PENDIENTE', 'CONFIRMADA'].includes(reserva.estado)) {
      throw errores.reglaNegocio('CANCELACION_NO_PERMITIDA', 'Solo se pueden cancelar reservas pendientes o confirmadas.');
    }
    const penalidad = calcularPenalidad(reserva, reloj());
    store.actualizar('reservas', reserva.id, {
      estado: 'CANCELADA',
      penalidad,
      canceladaPor: req.usuario.uid === reserva.arrendadorId ? 'ARRENDADOR' : 'ARRENDATARIO',
      actualizadoEn: reloj().toISOString(),
    });
    res.json(reservaDto(req, store, store.obtener('reservas', reserva.id), req.usuario.uid));
  });

  // PATCH /api/reservas/:id/estado { estado } -> flujo del arrendador
  r.patch('/:id/estado', autenticar, (req, res) => {
    const reserva = reservaParticipante(req);
    if (reserva.arrendadorId !== req.usuario.uid) {
      throw errores.prohibido('Solo la empresa arrendadora puede cambiar el estado de la reserva.');
    }
    const nuevo = req.body?.estado;
    const permitidos = TRANSICIONES_ARRENDADOR[reserva.estado] ?? [];
    if (!permitidos.includes(nuevo)) {
      throw errores.reglaNegocio('TRANSICION_NO_PERMITIDA', `No se puede pasar de ${reserva.estado} a ${nuevo ?? '(vacío)'}.`);
    }
    const ahora = reloj().toISOString();
    store.actualizar('reservas', reserva.id, {
      estado: nuevo,
      ...(nuevo === 'DEVUELTA' ? { devueltaEn: ahora } : {}),
      actualizadoEn: ahora,
    });
    res.json(reservaDto(req, store, store.obtener('reservas', reserva.id), req.usuario.uid));
  });

  // POST /api/reservas/:id/resenas (RN-05 y RN-06)
  r.post('/:id/resenas', autenticar, (req, res) => {
    const reserva = reservaParticipante(req);
    const ventana = verificarVentanaResena(reserva, reloj());
    if (!ventana.permitido) throw errores.reglaNegocio('RESENA_FUERA_DE_PLAZO', ventana.motivo);
    const { errores: lista, datos } = validarResena(req.body);
    if (lista.length) throw errores.validacion(lista);
    const uid = req.usuario.uid;
    const yaExiste = store.listar('resenas', (x) => x.reservaId === reserva.id && x.autorId === uid).length > 0;
    if (yaExiste) throw errores.conflicto('RESENA_DUPLICADA', 'Ya calificaste esta reserva.');

    const esArrendatario = uid === reserva.arrendatarioId;
    const resena = store.insertar('resenas', {
      id: crypto.randomUUID(),
      reservaId: reserva.id,
      autorId: uid,
      destinatarioId: esArrendatario ? reserva.arrendadorId : reserva.arrendatarioId,
      equipoId: reserva.equipoId,
      tipo: esArrendatario ? 'A_EQUIPO' : 'A_ARRENDATARIO',
      ...datos,
      creadoEn: reloj().toISOString(),
    });

    if (esArrendatario) {
      const estrellas = store
        .listar('resenas', (x) => x.equipoId === reserva.equipoId && x.tipo === 'A_EQUIPO')
        .map((x) => x.estrellas);
      const prom = promedio(estrellas);
      const equipo = store.obtener('equipos', reserva.equipoId);
      if (equipo) {
        store.actualizar('equipos', equipo.id, {
          calificacionPromedio: prom,
          totalResenas: estrellas.length,
          ...(requiereRevision(prom, estrellas.length) ? { estado: 'EN_REVISION' } : {}),
        });
      }
    } else {
      const estrellas = store
        .listar('resenas', (x) => x.destinatarioId === reserva.arrendatarioId && x.tipo === 'A_ARRENDATARIO')
        .map((x) => x.estrellas);
      store.actualizar('empresas', reserva.arrendatarioId, {
        calificacionComoArrendatario: promedio(estrellas),
        totalResenasComoArrendatario: estrellas.length,
      });
    }
    res.status(201).json({ id: resena.id, estrellas: resena.estrellas, comentario: resena.comentario, tipo: resena.tipo, creadoEn: resena.creadoEn });
  });

  return r;
}
