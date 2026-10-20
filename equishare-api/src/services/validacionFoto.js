// Validación automática de fotos (Fase 1, RN-04). Sustituye por ahora al modelo de Machine
// Learning, que queda como fase futura. Cada rechazo devuelve un motivo explícito para que la
// empresa sepa qué corregir (principio de transparencia del Anexo de marketing ético).
import { imageSize } from 'image-size';

export const TAMANO_MAX_BYTES = 5 * 1024 * 1024;
export const LADO_MAYOR_MIN = 640;
export const LADO_MENOR_MIN = 480;
const TIPOS_PERMITIDOS = { jpg: 'image/jpeg', png: 'image/png', webp: 'image/webp' };

export function validarFoto(buffer) {
  const motivos = [];
  if (!buffer || buffer.length === 0) {
    return { aprobada: false, motivos: ['No se recibió ninguna imagen.'], tipo: null };
  }
  if (buffer.length > TAMANO_MAX_BYTES) {
    motivos.push('La imagen supera el tamaño máximo de 5 MB.');
  }
  let info = null;
  try {
    info = imageSize(new Uint8Array(buffer));
  } catch {
    return { aprobada: false, motivos: ['El archivo no es una imagen válida.'], tipo: null };
  }
  const tipo = TIPOS_PERMITIDOS[info.type] ?? null;
  if (!tipo) {
    motivos.push('Formato no permitido. Usa JPG, PNG o WebP.');
  }
  // Las fotos de cámara pueden venir rotadas (orientación EXIF 5-8): se usan los lados reales.
  const rotada = info.orientation && info.orientation >= 5;
  const ancho = rotada ? info.height : info.width;
  const alto = rotada ? info.width : info.height;
  const mayor = Math.max(ancho, alto);
  const menor = Math.min(ancho, alto);
  if (mayor < LADO_MAYOR_MIN || menor < LADO_MENOR_MIN) {
    motivos.push(`Resolución insuficiente (${ancho}x${alto}). Mínimo 640x480 píxeles para que el equipo se vea con claridad.`);
  }
  const proporcion = mayor / Math.max(menor, 1);
  if (proporcion > 2) {
    motivos.push('La imagen es demasiado alargada (proporción mayor a 2:1). Encuadra el equipo completo.');
  }
  return {
    aprobada: motivos.length === 0,
    motivos,
    tipo,
    extension: info.type === 'jpg' ? 'jpg' : info.type,
    ancho,
    alto,
  };
}
