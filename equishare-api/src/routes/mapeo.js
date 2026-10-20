// Conversión de entidades internas a los objetos JSON del contrato de la API.
export function urlBase(req) {
  return `${req.protocol}://${req.get('host')}`;
}

export function urlFoto(req, store, fotoId) {
  if (!fotoId) return null;
  const foto = store.obtener('fotos', fotoId);
  return foto ? `${urlBase(req)}${foto.ruta}` : null;
}

export function equipoDto(req, store, e, extra = {}) {
  const empresa = store.obtener('empresas', e.empresaId);
  return {
    id: e.id,
    empresaId: e.empresaId,
    empresaNombre: empresa?.razonSocial ?? 'Empresa',
    nombre: e.nombre,
    categoria: e.categoria,
    descripcion: e.descripcion,
    precioDia: e.precioDia,
    fotoUrl: urlFoto(req, store, e.fotoId),
    fotoId: e.fotoId ?? null,
    lat: e.lat,
    lng: e.lng,
    direccion: e.direccion,
    estado: e.estado,
    calificacionPromedio: e.calificacionPromedio ?? 0,
    totalResenas: e.totalResenas ?? 0,
    creadoEn: e.creadoEn,
    actualizadoEn: e.actualizadoEn,
    distanciaKm: null,
    ...extra,
  };
}

export function reservaDto(req, store, r, uidSolicitante) {
  const equipo = store.obtener('equipos', r.equipoId);
  const arrendatario = store.obtener('empresas', r.arrendatarioId);
  const arrendador = store.obtener('empresas', r.arrendadorId);
  const resenaPropia = store
    .listar('resenas', (x) => x.reservaId === r.id && x.autorId === uidSolicitante)
    .at(0);
  return {
    id: r.id,
    equipoId: r.equipoId,
    equipoNombre: equipo?.nombre ?? r.equipoNombre,
    equipoFotoUrl: equipo ? urlFoto(req, store, equipo.fotoId) : null,
    arrendatarioId: r.arrendatarioId,
    arrendatarioNombre: arrendatario?.razonSocial ?? 'Empresa',
    arrendadorId: r.arrendadorId,
    arrendadorNombre: arrendador?.razonSocial ?? 'Empresa',
    fechaInicio: r.fechaInicio,
    fechaFin: r.fechaFin,
    dias: r.dias,
    precioDia: r.precioDia,
    subtotal: r.subtotal,
    comision: r.comision,
    montoArrendador: r.montoArrendador,
    total: r.total,
    penalidad: r.penalidad ?? 0,
    estado: r.estado,
    canceladaPor: r.canceladaPor ?? null,
    devueltaEn: r.devueltaEn ?? null,
    creadoEn: r.creadoEn,
    rolSolicitante: uidSolicitante === r.arrendadorId ? 'ARRENDADOR' : 'ARRENDATARIO',
    resenaEnviada: Boolean(resenaPropia),
  };
}

export function empresaDto(e) {
  return {
    id: e.id,
    razonSocial: e.razonSocial,
    ruc: e.ruc,
    telefono: e.telefono,
    email: e.email,
    verificada: e.verificada,
    calificacionComoArrendatario: e.calificacionComoArrendatario ?? 0,
    totalResenasComoArrendatario: e.totalResenasComoArrendatario ?? 0,
    creadoEn: e.creadoEn,
  };
}
