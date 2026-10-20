import { Router } from 'express';
import { errores } from '../errors.js';
import { validarRuc } from '../domain/ruc.js';
import { validarEmpresa } from '../domain/validaciones.js';
import { empresaDto } from './mapeo.js';

export function rutasEmpresas({ store, autenticar, reloj }) {
  const r = Router();

  // RN-01: registro de la empresa con verificación del RUC.
  r.put('/me', autenticar, (req, res) => {
    const { errores: lista, datos } = validarEmpresa(req.body);
    const ruc = String(req.body?.ruc ?? '').trim();
    const resultadoRuc = validarRuc(ruc);
    if (!resultadoRuc.valido) lista.push({ campo: 'ruc', mensaje: resultadoRuc.motivo });
    if (lista.length) throw errores.validacion(lista);

    const duplicado = store.listar('empresas', (e) => e.ruc === ruc && e.id !== req.usuario.uid).length > 0;
    if (duplicado) throw errores.conflicto('RUC_DUPLICADO', 'Ya existe otra empresa registrada con ese RUC.');

    const existente = store.obtener('empresas', req.usuario.uid);
    const ahora = reloj().toISOString();
    if (existente) {
      store.actualizar('empresas', existente.id, { ...datos, ruc, verificada: true, actualizadoEn: ahora });
      return res.json(empresaDto(store.obtener('empresas', existente.id)));
    }
    const empresa = store.insertar('empresas', {
      id: req.usuario.uid,
      email: req.usuario.email,
      ruc,
      ...datos,
      verificada: true,
      calificacionComoArrendatario: 0,
      totalResenasComoArrendatario: 0,
      creadoEn: ahora,
      actualizadoEn: ahora,
    });
    return res.status(201).json(empresaDto(empresa));
  });

  r.get('/me', autenticar, (req, res) => {
    const empresa = store.obtener('empresas', req.usuario.uid);
    if (!empresa) throw errores.noEncontrado('El perfil de empresa');
    res.json(empresaDto(empresa));
  });

  r.get('/:id', (req, res) => {
    const empresa = store.obtener('empresas', req.params.id);
    if (!empresa) throw errores.noEncontrado('La empresa');
    const { telefono, email, ...publica } = empresaDto(empresa);
    const equiposPublicados = store.listar(
      'equipos',
      (e) => e.empresaId === empresa.id && e.estado === 'PUBLICADO'
    ).length;
    res.json({ ...publica, equiposPublicados });
  });

  return r;
}

/** Middleware RN-01: la empresa debe existir y tener el RUC verificado. */
export function requiereEmpresaVerificada(store) {
  return (req, _res, next) => {
    const empresa = store.obtener('empresas', req.usuario.uid);
    if (!empresa || !empresa.verificada) {
      return next(
        errores.reglaNegocio(
          'EMPRESA_NO_VERIFICADA',
          'Registra tu empresa y verifica tu RUC antes de publicar o alquilar equipos.'
        )
      );
    }
    req.empresa = empresa;
    return next();
  };
}
