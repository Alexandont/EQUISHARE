// Validaciones de entrada de la API (equivalentes a las que hace la app en el cliente).
export const CATEGORIAS = ['CONSTRUCCION', 'EVENTOS', 'HERRAMIENTAS', 'TECNOLOGIA'];

const texto = (v) => (typeof v === 'string' ? v.trim() : '');

export function validarEmpresa(body) {
  const errores = [];
  const razonSocial = texto(body?.razonSocial);
  const telefono = texto(body?.telefono);
  if (razonSocial.length < 3 || razonSocial.length > 120) {
    errores.push({ campo: 'razonSocial', mensaje: 'La razón social debe tener entre 3 y 120 caracteres.' });
  }
  if (!/^9\d{8}$/.test(telefono)) {
    errores.push({ campo: 'telefono', mensaje: 'El teléfono debe ser un celular peruano de 9 dígitos que empieza con 9.' });
  }
  return { errores, datos: { razonSocial, telefono } };
}

export function validarEquipo(body, { parcial = false } = {}) {
  const errores = [];
  const datos = {};
  const presente = (campo) => body?.[campo] !== undefined;

  if (!parcial || presente('nombre')) {
    const nombre = texto(body?.nombre);
    if (nombre.length < 3 || nombre.length > 80) {
      errores.push({ campo: 'nombre', mensaje: 'El nombre debe tener entre 3 y 80 caracteres.' });
    }
    datos.nombre = nombre;
  }
  if (!parcial || presente('descripcion')) {
    const descripcion = texto(body?.descripcion);
    if (descripcion.length < 10 || descripcion.length > 500) {
      errores.push({ campo: 'descripcion', mensaje: 'La descripción debe tener entre 10 y 500 caracteres.' });
    }
    datos.descripcion = descripcion;
  }
  if (!parcial || presente('categoria')) {
    if (!CATEGORIAS.includes(body?.categoria)) {
      errores.push({ campo: 'categoria', mensaje: `La categoría debe ser una de: ${CATEGORIAS.join(', ')}.` });
    }
    datos.categoria = body?.categoria;
  }
  if (!parcial || presente('precioDia')) {
    const precio = Number(body?.precioDia);
    if (!Number.isFinite(precio) || precio < 1 || precio > 50000) {
      errores.push({ campo: 'precioDia', mensaje: 'El precio por día debe estar entre S/ 1 y S/ 50 000.' });
    }
    datos.precioDia = Math.round(precio * 100) / 100;
  }
  if (!parcial || presente('lat') || presente('lng')) {
    const lat = Number(body?.lat);
    const lng = Number(body?.lng);
    if (!Number.isFinite(lat) || lat < -90 || lat > 90 || !Number.isFinite(lng) || lng < -180 || lng > 180) {
      errores.push({ campo: 'ubicacion', mensaje: 'La ubicación (lat, lng) no es válida.' });
    }
    datos.lat = lat;
    datos.lng = lng;
  }
  if (!parcial || presente('direccion')) {
    const direccion = texto(body?.direccion);
    if (direccion.length < 5 || direccion.length > 150) {
      errores.push({ campo: 'direccion', mensaje: 'La dirección debe tener entre 5 y 150 caracteres.' });
    }
    datos.direccion = direccion;
  }
  if (presente('fotoId')) {
    datos.fotoId = body.fotoId === null ? null : texto(body.fotoId);
  }
  if (presente('visible')) {
    if (typeof body.visible !== 'boolean') {
      errores.push({ campo: 'visible', mensaje: 'visible debe ser verdadero o falso.' });
    }
    datos.visible = body.visible;
  }
  return { errores, datos };
}

export function validarResena(body) {
  const errores = [];
  const estrellas = Number(body?.estrellas);
  const comentario = texto(body?.comentario);
  if (!Number.isInteger(estrellas) || estrellas < 1 || estrellas > 5) {
    errores.push({ campo: 'estrellas', mensaje: 'La calificación debe ser un entero de 1 a 5.' });
  }
  if (comentario.length < 3 || comentario.length > 300) {
    errores.push({ campo: 'comentario', mensaje: 'El comentario debe tener entre 3 y 300 caracteres.' });
  }
  return { errores, datos: { estrellas, comentario } };
}
