// =============================================================================
// EQUISHARE - Evidencia reproducible de consumo de la API REST (Figura 12)
//
// Ejecuta contra el servidor Node.js en funcionamiento la misma secuencia de
// peticiones documentada en docs/evidencia_api_curl.txt y muestra la respuesta
// real de cada una con su codigo HTTP. Sirve para capturar la figura con datos
// reales en lugar de un ejemplo transcrito.
//
// USO (PC1):
//   1) En una ventana:   cd equishare-api  &&  npm start
//   2) En otra ventana:  node docs/evidencia_api.mjs
//
// Opciones:
//   --puerto=3000        Puerto donde escucha la API (el que imprime npm start).
//   --equipo=eq-demo-1   Equipo de demostracion usado en cotizacion y reservas.
//
// No requiere dependencias: usa fetch y zlib de Node (>= 20.19).
// =============================================================================

import { deflateSync } from 'node:zlib';
import { readFileSync, existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const argumento = (nombre, pordefecto) => {
  const encontrado = process.argv.find((a) => a.startsWith(`--${nombre}=`));
  return encontrado ? encontrado.split('=').slice(1).join('=') : pordefecto;
};

const PUERTO = Number(argumento('puerto', 3000));
const EQUIPO = argumento('equipo', 'eq-demo-1');
const BASE = `http://localhost:${PUERTO}`;
const TOKEN = 'Bearer demo:empresa-prueba';
const TOKEN_OTRA_EMPRESA = 'Bearer demo:demo-eventos';

const raizRepo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

// --- Colores ANSI para que la captura se lea bien ----------------------------
const c = {
  reset: '\u001b[0m',
  gris: '\u001b[90m',
  cian: '\u001b[36m',
  verde: '\u001b[32m',
  rojo: '\u001b[31m',
  amarillo: '\u001b[33m',
};

// --- Imagen PNG solida generada al vuelo (sin dependencias) ------------------
function pngSolido(ancho, alto) {
  const crc32 = (buf) => {
    let crc = ~0;
    for (const b of buf) {
      crc ^= b;
      for (let k = 0; k < 8; k += 1) crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
    }
    return ~crc >>> 0;
  };
  const bloque = (tipo, datos) => {
    const largo = Buffer.alloc(4);
    largo.writeUInt32BE(datos.length);
    const cuerpo = Buffer.concat([Buffer.from(tipo, 'ascii'), datos]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(cuerpo));
    return Buffer.concat([largo, cuerpo, crc]);
  };
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(ancho, 0);
  ihdr.writeUInt32BE(alto, 4);
  ihdr[8] = 8; // profundidad de bits
  ihdr[9] = 2; // color RGB
  const fila = Buffer.concat([Buffer.from([0]), Buffer.alloc(ancho * 3, 0x9a)]);
  const datos = deflateSync(Buffer.concat(Array.from({ length: alto }, () => fila)));
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    bloque('IHDR', ihdr),
    bloque('IDAT', datos),
    bloque('IEND', Buffer.alloc(0)),
  ]);
}

// --- Fechas en hora de Lima (UTC-5), coherentes con RN-07 --------------------
const MS_DIA = 86_400_000;
const diaLima = (desplazamiento = 0) =>
  new Date(Date.now() - 5 * 3_600_000 + desplazamiento * MS_DIA).toISOString().slice(0, 10);

const HOY = diaLima(0);
const INICIO = diaLima(3);
const FIN = diaLima(5);

// --- Ejecutor de peticiones --------------------------------------------------
function imprimir(titulo, estado, texto) {
  const color = estado >= 500 ? c.rojo : estado >= 400 ? c.amarillo : c.verde;
  console.log('');
  console.log(`${c.cian}$ ${titulo}${c.reset}`);
  const cuerpo = texto.trim();
  if (cuerpo) console.log(`${cuerpo}  ${color}-> HTTP ${estado}${c.reset}`);
  else console.log(`${color}HTTP ${estado}${c.reset}`);
}

async function pedir(
  titulo,
  ruta,
  { metodo = 'GET', token = null, json = null, form = null, campos = null, silencioso = false } = {}
) {
  const cabeceras = {};
  if (token) cabeceras.Authorization = token;
  let cuerpo;
  if (json) {
    cabeceras['Content-Type'] = 'application/json';
    cuerpo = JSON.stringify(json);
  } else if (form) {
    cuerpo = form;
  }
  const respuesta = await fetch(`${BASE}${ruta}`, { method: metodo, headers: cabeceras, body: cuerpo });
  const texto = await respuesta.text();
  let datos = null;
  try {
    datos = JSON.parse(texto);
  } catch {
    datos = null;
  }
  if (!silencioso) {
    // Con "campos" se muestran solo las claves relevantes para que la linea quepa en la captura.
    const mostrado =
      campos && datos && respuesta.ok
        ? JSON.stringify(Object.fromEntries(campos.filter((k) => k in datos).map((k) => [k, datos[k]])))
        : texto;
    imprimir(titulo, respuesta.status, mostrado);
  }
  return datos;
}

function formulario(nombreArchivo, buffer) {
  const datos = new FormData();
  datos.append('foto', new Blob([buffer], { type: 'image/png' }), nombreArchivo);
  return datos;
}

// --- Secuencia ---------------------------------------------------------------
async function principal() {
  let salud;
  try {
    salud = await fetch(`${BASE}/api/health`, { signal: AbortSignal.timeout(5000) });
  } catch {
    console.error(
      `${c.rojo}No hay respuesta en ${BASE}. Levanta el servidor con "npm start" dentro de equishare-api.${c.reset}`
    );
    process.exit(1);
  }
  if (!salud.ok) {
    console.error(`${c.rojo}El servidor respondio ${salud.status} en /api/health.${c.reset}`);
    process.exit(1);
  }

  const marca = new Date().toISOString().slice(0, 16);
  console.log(`${c.amarillo}== Evidencia de consumo de la API REST EQUISHARE (${marca}Z) ==${c.reset}`);
  console.log(`${c.gris}   Servidor: ${BASE}   Equipo de prueba: ${EQUIPO}   Hoy en Lima: ${HOY}${c.reset}`);

  await pedir('GET /api/health', '/api/health');

  await pedir('PUT /api/empresas/me  (RUC con digito verificador incorrecto)', '/api/empresas/me', {
    metodo: 'PUT',
    token: TOKEN,
    json: { razonSocial: 'Empresa Prueba S.A.C.', ruc: '20131312954', telefono: '912345678' },
  });

  await pedir('PUT /api/empresas/me  (RUC valido)', '/api/empresas/me', {
    metodo: 'PUT',
    token: TOKEN,
    json: { razonSocial: 'Empresa Prueba S.A.C.', ruc: '20131312955', telefono: '912345678' },
  });

  await pedir('POST /api/equipos/fotos  (imagen de 300x200)', '/api/equipos/fotos', {
    metodo: 'POST',
    token: TOKEN,
    form: formulario('foto_300x200.png', pngSolido(300, 200)),
  });

  // Foto valida de 800x600: se reutiliza una imagen de demostracion del repositorio.
  const rutaFotoValida = path.join(raizRepo, 'equishare-api', 'seed', 'fotos', 'mezcladora.png');
  const fotoValida = existsSync(rutaFotoValida) ? readFileSync(rutaFotoValida) : pngSolido(800, 600);
  const foto = await pedir('POST /api/equipos/fotos  (imagen de 800x600)', '/api/equipos/fotos', {
    metodo: 'POST',
    token: TOKEN,
    form: formulario('foto_800x600.png', fotoValida),
  });

  const datosEquipo = {
    nombre: 'Cortadora de cerámica',
    categoria: 'HERRAMIENTAS',
    descripcion: 'Cortadora eléctrica con disco diamantado de 7 pulgadas.',
    precioDia: 35,
    fotoId: foto?.id,
    lat: -8.11,
    lng: -79.03,
    direccion: 'Centro de Trujillo',
  };
  const equipo = await pedir('POST /api/equipos', '/api/equipos', {
    metodo: 'POST',
    token: TOKEN,
    json: datosEquipo,
    campos: ['id', 'empresaNombre', 'nombre', 'categoria', 'precioDia', 'fotoUrl', 'estado'],
  });

  if (equipo?.id) {
    await pedir(`PUT /api/equipos/${equipo.id}  (actualizar precio)`, `/api/equipos/${equipo.id}`, {
      metodo: 'PUT',
      token: TOKEN,
      json: { ...datosEquipo, precioDia: 40, fotoId: undefined },
      campos: ['id', 'nombre', 'precioDia', 'estado'],
    });
  }

  const rango = { equipoId: EQUIPO, fechaInicio: INICIO, fechaFin: FIN };

  await pedir(`POST /api/reservas/cotizacion  (${INICIO} a ${FIN})`, '/api/reservas/cotizacion', {
    metodo: 'POST',
    token: TOKEN,
    json: rango,
  });

  // RN-07: el inicio de hoy nunca alcanza las 24 h de anticipacion respecto de las 08:00.
  await pedir(`POST /api/reservas  (inicio hoy ${HOY}: incumple RN-07)`, '/api/reservas', {
    metodo: 'POST',
    token: TOKEN,
    json: { equipoId: EQUIPO, fechaInicio: HOY, fechaFin: HOY },
  });

  const reserva = await pedir(`POST /api/reservas  (${INICIO} a ${FIN})`, '/api/reservas', {
    metodo: 'POST',
    token: TOKEN,
    json: rango,
    campos: ['id', 'equipoNombre', 'fechaInicio', 'fechaFin', 'dias', 'subtotal', 'comision', 'montoArrendador', 'total', 'estado'],
  });

  // RN-02: otra empresa pide exactamente el mismo rango sobre el mismo equipo.
  await pedir('POST /api/reservas  (mismas fechas, otra empresa: RN-02)', '/api/reservas', {
    metodo: 'POST',
    token: TOKEN_OTRA_EMPRESA,
    json: rango,
  });

  if (equipo?.id) {
    await pedir(`DELETE /api/equipos/${equipo.id}`, `/api/equipos/${equipo.id}`, {
      metodo: 'DELETE',
      token: TOKEN,
    });
    await pedir(`GET /api/equipos/${equipo.id}  (eliminado)`, `/api/equipos/${equipo.id}`);
  }

  console.log('');
  console.log(`${c.amarillo}== Fin de la evidencia ==${c.reset}`);

  // Limpieza silenciosa: se cancela la reserva creada para que el script pueda
  // volver a ejecutarse sobre la misma base de datos sin chocar con RN-02.
  if (reserva?.id) {
    await pedir('', `/api/reservas/${reserva.id}/cancelar`, {
      metodo: 'PATCH',
      token: TOKEN,
      silencioso: true,
    });
  }
}

principal().catch((error) => {
  console.error(`${c.rojo}Error inesperado: ${error.message}${c.reset}`);
  process.exit(1);
});
