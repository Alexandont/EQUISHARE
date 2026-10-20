// Reglas de negocio de EQUISHARE.
export const COMISION_PLATAFORMA = 0.12;
export const PENALIDAD_CANCELACION = 0.2;
export const ANTICIPACION_MINIMA_HORAS = 24;
export const LIMITE_CANCELACION_HORAS = 12;
export const VENTANA_RESENA_HORAS = 48;
export const UMBRAL_REVISION = 3.0;
export const MIN_RESENAS_REVISION = 5;
export const MAX_DIAS_RESERVA = 90;
export const HORA_ENTREGA = '08:00:00';
export const OFFSET_LIMA = '-05:00'; // Perú no aplica horario de verano

export const ESTADOS_ACTIVOS = ['PENDIENTE', 'CONFIRMADA', 'EN_CURSO'];

const MS_HORA = 3_600_000;
const MS_DIA = 86_400_000;

export function redondear2(valor) {
  return Math.round((valor + Number.EPSILON) * 100) / 100;
}

export function esFechaIso(valor) {
  if (typeof valor !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(valor)) return false;
  const d = new Date(`${valor}T00:00:00Z`);
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === valor;
}

/** Instante de entrega del equipo: 08:00 hora de Lima del día de inicio. */
export function instanteInicio(fechaInicio) {
  return new Date(`${fechaInicio}T${HORA_ENTREGA}${OFFSET_LIMA}`);
}

/** Número de días de alquiler (ambas fechas incluidas). */
export function diasAlquiler(fechaInicio, fechaFin) {
  const a = Date.parse(`${fechaInicio}T00:00:00Z`);
  const b = Date.parse(`${fechaFin}T00:00:00Z`);
  return Math.round((b - a) / MS_DIA) + 1;
}

/** La comisión (12 %) se descuenta al arrendador; el arrendatario paga el subtotal. */
export function cotizar(precioDia, fechaInicio, fechaFin) {
  const dias = diasAlquiler(fechaInicio, fechaFin);
  const subtotal = redondear2(precioDia * dias);
  const comision = redondear2(subtotal * COMISION_PLATAFORMA);
  return {
    dias,
    precioDia: redondear2(precioDia),
    subtotal,
    comision,
    montoArrendador: redondear2(subtotal - comision),
    total: subtotal,
  };
}

/** Devuelve la lista de errores de las fechas; vacía si son válidas (incluye la anticipación mínima). */
export function validarFechasReserva(fechaInicio, fechaFin, ahora = new Date()) {
  const errores = [];
  if (!esFechaIso(fechaInicio)) errores.push('fechaInicio debe tener formato AAAA-MM-DD.');
  if (!esFechaIso(fechaFin)) errores.push('fechaFin debe tener formato AAAA-MM-DD.');
  if (errores.length) return errores;
  const dias = diasAlquiler(fechaInicio, fechaFin);
  if (dias < 1) errores.push('La fecha de fin no puede ser anterior a la fecha de inicio.');
  if (dias > MAX_DIAS_RESERVA) errores.push(`Una reserva no puede superar ${MAX_DIAS_RESERVA} días.`);
  const horasAnticipacion = (instanteInicio(fechaInicio).getTime() - ahora.getTime()) / MS_HORA;
  if (horasAnticipacion < ANTICIPACION_MINIMA_HORAS) {
    errores.push(
      `La reserva debe hacerse con al menos ${ANTICIPACION_MINIMA_HORAS} horas de anticipación (entrega a las 08:00 del día de inicio).`
    );
  }
  return errores;
}

/** Dos rangos de días (incluidos) se cruzan si a.inicio <= b.fin y b.inicio <= a.fin. */
export function seSolapan(inicioA, finA, inicioB, finB) {
  return inicioA <= finB && inicioB <= finA;
}

export function haySolapamiento(reservasEquipo, fechaInicio, fechaFin, excluirId = null) {
  return reservasEquipo.some(
    (r) =>
      r.id !== excluirId &&
      ESTADOS_ACTIVOS.includes(r.estado) &&
      seSolapan(r.fechaInicio, r.fechaFin, fechaInicio, fechaFin)
  );
}

/** Penalidad del 20 % si se cancela con menos de 12 horas de anticipación. */
export function calcularPenalidad(reserva, ahora = new Date()) {
  const horas = (instanteInicio(reserva.fechaInicio).getTime() - ahora.getTime()) / MS_HORA;
  if (horas < LIMITE_CANCELACION_HORAS) {
    return redondear2(reserva.total * PENALIDAD_CANCELACION);
  }
  return 0;
}

/** Solo se califica una reserva devuelta y dentro de las 48 horas siguientes. */
export function verificarVentanaResena(reserva, ahora = new Date()) {
  if (reserva.estado !== 'DEVUELTA' || !reserva.devueltaEn) {
    return { permitido: false, motivo: 'Solo se puede calificar una reserva cuyo equipo ya fue devuelto.' };
  }
  const horas = (ahora.getTime() - new Date(reserva.devueltaEn).getTime()) / MS_HORA;
  if (horas > VENTANA_RESENA_HORAS) {
    return { permitido: false, motivo: `El plazo de ${VENTANA_RESENA_HORAS} horas para calificar ya venció.` };
  }
  return { permitido: true, motivo: null };
}

/** Revisión automática si el promedio es menor a 3.0 tras al menos 5 reseñas. */
export function requiereRevision(promedio, totalResenas) {
  return totalResenas >= MIN_RESENAS_REVISION && promedio < UMBRAL_REVISION;
}

export function promedio(valores) {
  if (!valores.length) return 0;
  return redondear2(valores.reduce((a, b) => a + b, 0) / valores.length);
}

/** Distancia en km entre dos coordenadas (fórmula del haversine). */
export function distanciaKm(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const rad = (g) => (g * Math.PI) / 180;
  const dLat = rad(lat2 - lat1);
  const dLng = rad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2;
  return redondear2(2 * R * Math.asin(Math.sqrt(a)));
}

/** Transiciones de estado permitidas al arrendador. */
export const TRANSICIONES_ARRENDADOR = {
  PENDIENTE: ['CONFIRMADA'],
  CONFIRMADA: ['EN_CURSO'],
  EN_CURSO: ['DEVUELTA'],
};
