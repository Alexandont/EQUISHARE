import { Router } from 'express';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import multer from 'multer';
import { errores, ApiError } from '../errors.js';
import { CATEGORIAS, validarEquipo } from '../domain/validaciones.js';
import { distanciaKm, ESTADOS_ACTIVOS } from '../domain/reglas.js';
import { validarFoto, TAMANO_MAX_BYTES } from '../services/validacionFoto.js';
import { requiereEmpresaVerificada } from './empresas.js';
import { equipoDto } from './mapeo.js';

export function rutasEquipos({ store, autenticar, reloj, carpetaUploads }) {
  const r = Router();
  const verificada = requiereEmpresaVerificada(store);
  const subida = multer({ storage: multer.memoryStorage(), limits: { fileSize: TAMANO_MAX_BYTES, files: 1 } });

  function equipoPropio(req) {
    const equipo = store.obtener('equipos', req.params.id);
    if (!equipo) throw errores.noEncontrado('El equipo');
    if (equipo.empresaId !== req.usuario.uid) throw errores.prohibido('Solo la empresa dueña puede modificar este equipo.');
    return equipo;
  }

  function fotoAprobadaDe(fotoId, uid) {
    const foto = store.obtener('fotos', fotoId);
    if (!foto || foto.empresaId !== uid || !foto.aprobada) {
      throw errores.reglaNegocio('FOTO_NO_VALIDA', 'La foto indicada no existe o no fue aprobada por la validación automática.');
    }
    return foto;
  }

  // GET /api/equipos  -> catálogo público (solo equipos PUBLICADO)
  r.get('/', (req, res) => {
    const q = String(req.query.q ?? '').trim().toLowerCase();
    const categoria = req.query.categoria ? String(req.query.categoria) : null;
    if (categoria && !CATEGORIAS.includes(categoria)) {
      throw errores.validacion([{ campo: 'categoria', mensaje: 'Categoría no válida.' }]);
    }
    const lat = req.query.lat !== undefined ? Number(req.query.lat) : null;
    const lng = req.query.lng !== undefined ? Number(req.query.lng) : null;
    const radioKm = req.query.radioKm !== undefined ? Number(req.query.radioKm) : null;
    const conUbicacion = Number.isFinite(lat) && Number.isFinite(lng);
    const page = Math.max(1, Number.parseInt(req.query.page ?? '1', 10) || 1);
    const size = Math.min(100, Math.max(1, Number.parseInt(req.query.size ?? '50', 10) || 50));

    let items = store
      .listar('equipos', (e) => e.estado === 'PUBLICADO')
      .filter((e) => !categoria || e.categoria === categoria)
      .filter((e) => !q || `${e.nombre} ${e.descripcion}`.toLowerCase().includes(q))
      .map((e) => equipoDto(req, store, e, conUbicacion ? { distanciaKm: distanciaKm(lat, lng, e.lat, e.lng) } : {}));

    if (conUbicacion) {
      if (Number.isFinite(radioKm) && radioKm > 0) items = items.filter((e) => e.distanciaKm <= radioKm);
      items.sort((a, b) => a.distanciaKm - b.distanciaKm);
    } else {
      items.sort((a, b) => b.creadoEn.localeCompare(a.creadoEn));
    }
    const total = items.length;
    res.json({ items: items.slice((page - 1) * size, page * size), page, size, total });
  });

  // GET /api/equipos/mios -> equipos de la empresa autenticada (cualquier estado)
  r.get('/mios', autenticar, (req, res) => {
    const items = store
      .listar('equipos', (e) => e.empresaId === req.usuario.uid)
      .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn))
      .map((e) => equipoDto(req, store, e));
    res.json({ items, page: 1, size: items.length, total: items.length });
  });

  // POST /api/equipos/fotos -> subida y validación automática de la foto
  r.post('/fotos', autenticar, verificada, (req, res, next) => {
    subida.single('foto')(req, res, (err) => {
      if (err instanceof multer.MulterError && err.code === 'LIMIT_FILE_SIZE') {
        return next(new ApiError(413, 'FOTO_MUY_PESADA', 'La imagen supera el tamaño máximo de 5 MB.'));
      }
      if (err) return next(errores.validacion([{ campo: 'foto', mensaje: 'No se pudo leer el archivo enviado.' }]));
      if (!req.file) return next(errores.validacion([{ campo: 'foto', mensaje: 'Adjunta una imagen en el campo "foto".' }]));

      const resultado = validarFoto(req.file.buffer);
      if (!resultado.aprobada) {
        return next(errores.reglaNegocio('FOTO_RECHAZADA', 'La foto no pasó la validación automática.', resultado.motivos));
      }
      const id = crypto.randomUUID();
      const nombreArchivo = `${id}.${resultado.extension}`;
      fs.mkdirSync(carpetaUploads, { recursive: true });
      fs.writeFileSync(path.join(carpetaUploads, nombreArchivo), req.file.buffer);
      const foto = store.insertar('fotos', {
        id,
        empresaId: req.usuario.uid,
        ruta: `/uploads/${nombreArchivo}`,
        aprobada: true,
        ancho: resultado.ancho,
        alto: resultado.alto,
        creadoEn: reloj().toISOString(),
      });
      return res.status(201).json({
        id: foto.id,
        url: `${req.protocol}://${req.get('host')}${foto.ruta}`,
        aprobada: true,
        motivos: [],
      });
    });
  });

  // GET /api/equipos/:id
  r.get('/:id', (req, res) => {
    const equipo = store.obtener('equipos', req.params.id);
    if (!equipo) throw errores.noEncontrado('El equipo');
    res.json(equipoDto(req, store, equipo));
  });

  // GET /api/equipos/:id/disponibilidad -> rangos ocupados
  r.get('/:id/disponibilidad', (req, res) => {
    const equipo = store.obtener('equipos', req.params.id);
    if (!equipo) throw errores.noEncontrado('El equipo');
    const ocupados = store
      .listar('reservas', (x) => x.equipoId === equipo.id && ESTADOS_ACTIVOS.includes(x.estado))
      .map((x) => ({ fechaInicio: x.fechaInicio, fechaFin: x.fechaFin }))
      .sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio));
    res.json({ equipoId: equipo.id, ocupados });
  });

  // GET /api/equipos/:id/resenas
  r.get('/:id/resenas', (req, res) => {
    const equipo = store.obtener('equipos', req.params.id);
    if (!equipo) throw errores.noEncontrado('El equipo');
    const items = store
      .listar('resenas', (x) => x.equipoId === equipo.id && x.tipo === 'A_EQUIPO')
      .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn))
      .map((x) => ({
        id: x.id,
        autorNombre: store.obtener('empresas', x.autorId)?.razonSocial ?? 'Empresa',
        estrellas: x.estrellas,
        comentario: x.comentario,
        creadoEn: x.creadoEn,
      }));
    res.json({ items, promedio: equipo.calificacionPromedio ?? 0, total: equipo.totalResenas ?? 0 });
  });

  // POST /api/equipos -> crear
  r.post('/', autenticar, verificada, (req, res) => {
    const { errores: lista, datos } = validarEquipo(req.body);
    if (lista.length) throw errores.validacion(lista);
    if (datos.fotoId) fotoAprobadaDe(datos.fotoId, req.usuario.uid);
    const ahora = reloj().toISOString();
    const visible = datos.visible ?? true;
    delete datos.visible;
    const equipo = store.insertar('equipos', {
      id: crypto.randomUUID(),
      empresaId: req.usuario.uid,
      ...datos,
      fotoId: datos.fotoId ?? null,
      // Sin foto aprobada el equipo no es visible en el catálogo.
      estado: !datos.fotoId ? 'PENDIENTE_FOTO' : visible ? 'PUBLICADO' : 'OCULTO',
      calificacionPromedio: 0,
      totalResenas: 0,
      creadoEn: ahora,
      actualizadoEn: ahora,
    });
    res.status(201).location(`/api/equipos/${equipo.id}`).json(equipoDto(req, store, equipo));
  });

  // PUT /api/equipos/:id -> actualizar (parcial permitido)
  r.put('/:id', autenticar, verificada, (req, res) => {
    const equipo = equipoPropio(req);
    const { errores: lista, datos } = validarEquipo(req.body, { parcial: true });
    if (lista.length) throw errores.validacion(lista);
    if (datos.fotoId) fotoAprobadaDe(datos.fotoId, req.usuario.uid);

    const fotoFinal = datos.fotoId !== undefined ? datos.fotoId : equipo.fotoId;
    const visible = datos.visible ?? equipo.estado !== 'OCULTO';
    delete datos.visible;
    let estado;
    if (equipo.estado === 'EN_REVISION') estado = 'EN_REVISION'; // Solo el administrador lo libera
    else if (!fotoFinal) estado = 'PENDIENTE_FOTO';
    else estado = visible ? 'PUBLICADO' : 'OCULTO';

    store.actualizar('equipos', equipo.id, { ...datos, fotoId: fotoFinal, estado, actualizadoEn: reloj().toISOString() });
    res.json(equipoDto(req, store, store.obtener('equipos', equipo.id)));
  });

  // DELETE /api/equipos/:id
  r.delete('/:id', autenticar, (req, res) => {
    const equipo = equipoPropio(req);
    const activas = store.listar('reservas', (x) => x.equipoId === equipo.id && ESTADOS_ACTIVOS.includes(x.estado));
    if (activas.length) {
      throw errores.conflicto('EQUIPO_CON_RESERVAS', 'No se puede eliminar un equipo con reservas activas. Cancélalas o espera a que terminen.');
    }
    store.eliminar('equipos', equipo.id);
    res.status(204).end();
  });

  return r;
}
