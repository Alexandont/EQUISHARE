// Datos de DEMOSTRACIÓN (empresas y equipos ficticios ubicados en Trujillo) para probar la app.
// Los precios son referenciales y no provienen de un estudio de mercado.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { crearStore } from './store.js';
import { cotizar } from '../domain/reglas.js';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');

const EMPRESAS = [
  { id: 'demo-constructora', razonSocial: 'Constructora Demo Norte S.A.C.', ruc: '20999999906', telefono: '987654321' },
  { id: 'demo-eventos', razonSocial: 'Eventos Demo Libertad E.I.R.L.', ruc: '20999999914', telefono: '976543210' },
  { id: 'demo-taller', razonSocial: 'Servicios Técnicos Demo S.R.L.', ruc: '20999999922', telefono: '965432109' },
];

const EQUIPOS = [
  ['demo-constructora', 'Mezcladora de concreto 9 p3', 'CONSTRUCCION', 'Mezcladora con motor a gasolina, ideal para obras pequeñas y medianas. Incluye manual de uso.', 80, -8.0791, -79.0041, 'Zona industrial, El Porvenir, Trujillo', 'mezcladora.png'],
  ['demo-constructora', 'Compactadora tipo plancha', 'CONSTRUCCION', 'Plancha compactadora vibratoria para afirmado y adoquinado. Se entrega con tanque lleno.', 90, -8.0779, -79.0432, 'Sector Manuel Arévalo, La Esperanza, Trujillo', 'compactadora.png'],
  ['demo-constructora', 'Andamio tubular (10 cuerpos)', 'CONSTRUCCION', 'Juego de 10 cuerpos de andamio con crucetas y plataformas metálicas.', 60, -8.1121, -79.0254, 'Centro histórico, Trujillo', 'andamio.png'],
  ['demo-constructora', 'Martillo demoledor eléctrico', 'HERRAMIENTAS', 'Martillo demoledor de 1500 W con dos cinceles. Uso con equipo de protección personal.', 70, -8.1718, -79.0093, 'Campiña de Moche, Trujillo', 'demoledor.png'],
  ['demo-eventos', 'Equipo de sonido 2000 W', 'EVENTOS', 'Dos parlantes activos, consola de 8 canales y dos micrófonos inalámbricos.', 150, -8.1339, -79.0452, 'Urb. California, Víctor Larco, Trujillo', 'sonido.png'],
  ['demo-eventos', 'Toldo modular 6 x 12 m', 'EVENTOS', 'Toldo blanco con estructura metálica para 80 personas. Incluye instalación básica.', 200, -8.0792, -79.1199, 'Huanchaco, Trujillo', 'toldo.png'],
  ['demo-eventos', 'Proyector Full HD + ecran', 'TECNOLOGIA', 'Proyector de 4000 lúmenes con ecran de 100 pulgadas y cable HDMI.', 60, -8.1093, -79.0289, 'Av. España, Trujillo', 'proyector.png'],
  ['demo-taller', 'Generador eléctrico 5 kW', 'HERRAMIENTAS', 'Generador a gasolina con arranque eléctrico y salidas de 220 V.', 120, -8.0889, -78.9601, 'Laredo, Trujillo', 'generador.png'],
  ['demo-taller', 'Taladro percutor inalámbrico', 'HERRAMIENTAS', 'Taladro de 20 V con dos baterías, cargador y juego de brocas.', 25, -8.0832, -79.0228, 'Florencia de Mora, Trujillo', 'taladro.png'],
  ['demo-taller', 'Escalera telescópica 6 m', 'HERRAMIENTAS', 'Escalera de aluminio de 6 metros con traba de seguridad, soporta 150 kg.', 20, -8.1201, -79.0362, 'Urb. La Merced, Trujillo', 'escalera.png'],
];

function fecha(base, deltaDias) {
  const d = new Date(base.getTime() + deltaDias * 86_400_000);
  return d.toISOString().slice(0, 10);
}

export function datosDemo(carpetaUploads, ahora = new Date()) {
  const iso = ahora.toISOString();
  const empresas = EMPRESAS.map((e) => ({
    ...e,
    email: `${e.id.replace('demo-', '')}@demo.pe`,
    verificada: true,
    calificacionComoArrendatario: 0,
    totalResenasComoArrendatario: 0,
    creadoEn: iso,
    actualizadoEn: iso,
  }));
  const fotos = [];
  const equipos = EQUIPOS.map(([empresaId, nombre, categoria, descripcion, precioDia, lat, lng, direccion, archivo], i) => {
    const fotoId = `foto-demo-${i + 1}`;
    const origen = path.join(raiz, 'seed', 'fotos', archivo);
    if (carpetaUploads && fs.existsSync(origen)) {
      fs.mkdirSync(carpetaUploads, { recursive: true });
      fs.copyFileSync(origen, path.join(carpetaUploads, archivo));
    }
    fotos.push({ id: fotoId, empresaId, ruta: `/uploads/${archivo}`, aprobada: true, ancho: 800, alto: 600, creadoEn: iso });
    return {
      id: `eq-demo-${i + 1}`,
      empresaId,
      nombre,
      categoria,
      descripcion,
      precioDia,
      lat,
      lng,
      direccion,
      fotoId,
      estado: 'PUBLICADO',
      calificacionPromedio: 0,
      totalResenas: 0,
      creadoEn: new Date(ahora.getTime() - i * 60_000).toISOString(),
      actualizadoEn: iso,
    };
  });

  // Historial de ejemplo: dos alquileres ya devueltos con su calificación.
  const reservas = [];
  const resenas = [];
  const historial = [
    ['eq-demo-1', 'demo-eventos', -12, -10, 5, 'Equipo en buen estado y entrega puntual.'],
    ['eq-demo-5', 'demo-constructora', -8, -7, 4, 'Buen sonido; la consola tenía un canal con ruido.'],
  ];
  historial.forEach(([equipoId, arrendatarioId, ini, fin, estrellas, comentario], i) => {
    const equipo = equipos.find((e) => e.id === equipoId);
    const fi = fecha(ahora, ini);
    const ff = fecha(ahora, fin);
    const id = `res-demo-${i + 1}`;
    reservas.push({
      id,
      equipoId,
      equipoNombre: equipo.nombre,
      arrendatarioId,
      arrendadorId: equipo.empresaId,
      fechaInicio: fi,
      fechaFin: ff,
      ...cotizar(equipo.precioDia, fi, ff),
      penalidad: 0,
      estado: 'DEVUELTA',
      devueltaEn: `${ff}T23:00:00.000Z`,
      creadoEn: `${fecha(ahora, ini - 3)}T15:00:00.000Z`,
      actualizadoEn: `${ff}T23:00:00.000Z`,
    });
    resenas.push({
      id: `resena-demo-${i + 1}`,
      reservaId: id,
      autorId: arrendatarioId,
      destinatarioId: equipo.empresaId,
      equipoId,
      tipo: 'A_EQUIPO',
      estrellas,
      comentario,
      creadoEn: `${ff}T23:30:00.000Z`,
    });
    equipo.calificacionPromedio = estrellas;
    equipo.totalResenas = 1;
  });

  return { empresas, equipos, fotos, reservas, resenas };
}

export function sembrarSiVacio(store, carpetaUploads) {
  if (store.listar('equipos').length > 0 || store.listar('empresas').length > 0) return false;
  const datos = datosDemo(carpetaUploads);
  for (const [coleccion, items] of Object.entries(datos)) {
    items.forEach((item) => store.insertar(coleccion, item));
  }
  console.log(`Datos de demostración cargados: ${datos.equipos.length} equipos, ${datos.empresas.length} empresas.`);
  return true;
}

// Ejecución directa: `npm run seed` reinicia la base con los datos de demostración.
if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const archivo = path.resolve(raiz, process.env.DATA_FILE ?? 'data/db.json');
  if (fs.existsSync(archivo)) fs.rmSync(archivo);
  const store = crearStore({ archivo });
  sembrarSiVacio(store, path.join(raiz, 'uploads'));
  console.log(`Base reiniciada en ${archivo}`);
}
