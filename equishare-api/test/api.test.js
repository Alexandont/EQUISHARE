import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import request from 'supertest';
import { crearApp } from '../src/app.js';
import { crearStore } from '../src/data/store.js';
import { datosDemo } from '../src/data/seed.js';

const dirTest = path.dirname(fileURLToPath(import.meta.url));
const fixture = (n) => path.join(dirTest, 'fixtures', n);

let ahoraMs;
let app;
const AUTH = (uid) => ({ Authorization: `Bearer demo:${uid}` });

beforeEach(() => {
  ahoraMs = Date.parse('2026-10-16T17:00:00Z'); // 12:00 en Lima
  const carpetaUploads = fs.mkdtempSync(path.join(os.tmpdir(), 'equishare-'));
  const store = crearStore({ datosIniciales: datosDemo(null, new Date(ahoraMs)) });
  app = crearApp({ store, carpetaUploads, reloj: () => new Date(ahoraMs), registrarPeticiones: false });
});

async function registrarEmpresa(uid, ruc = '20131312955') {
  return request(app).put('/api/empresas/me').set(AUTH(uid)).send({ ruc, razonSocial: 'Empresa de Prueba S.A.C.', telefono: '912345678' });
}

test('GET /api/health responde ok en modo demo', async () => {
  const res = await request(app).get('/api/health');
  assert.equal(res.status, 200);
  assert.equal(res.body.estado, 'ok');
  assert.equal(res.body.modoAutenticacion, 'demo');
});

test('Catálogo público: lista, filtra por categoría y búsqueda, y ordena por distancia', async () => {
  const todos = await request(app).get('/api/equipos');
  assert.equal(todos.status, 200);
  assert.equal(todos.body.total, 10);
  const eventos = await request(app).get('/api/equipos?categoria=EVENTOS');
  assert.ok(eventos.body.items.every((e) => e.categoria === 'EVENTOS'));
  const busqueda = await request(app).get('/api/equipos?q=generador');
  assert.equal(busqueda.body.total, 1);
  const cerca = await request(app).get('/api/equipos?lat=-8.1116&lng=-79.0287&radioKm=5');
  const distancias = cerca.body.items.map((e) => e.distanciaKm);
  assert.deepEqual(distancias, [...distancias].sort((a, b) => a - b));
  assert.ok(distancias.every((d) => d <= 5));
  const malaCategoria = await request(app).get('/api/equipos?categoria=AVIONES');
  assert.equal(malaCategoria.status, 400);
});

test('Errores HTTP con contrato JSON: 401 sin token, 404 inexistente, 400 JSON inválido', async () => {
  const sinToken = await request(app).get('/api/equipos/mios');
  assert.equal(sinToken.status, 401);
  assert.equal(sinToken.body.error.codigo, 'NO_AUTENTICADO');
  const noExiste = await request(app).get('/api/equipos/no-existe');
  assert.equal(noExiste.status, 404);
  const jsonMalo = await request(app)
    .put('/api/empresas/me')
    .set({ ...AUTH('x-123'), 'Content-Type': 'application/json' })
    .send('{"ruc": ');
  assert.equal(jsonMalo.status, 400);
  assert.equal(jsonMalo.body.error.codigo, 'JSON_INVALIDO');
});

test('RN-01: registro con RUC válido; RUC inválido y duplicado se rechazan', async () => {
  const malo = await registrarEmpresa('nueva-1', '20131312954');
  assert.equal(malo.status, 400);
  assert.ok(malo.body.error.detalles.some((d) => d.campo === 'ruc'));
  const ok = await registrarEmpresa('nueva-1');
  assert.equal(ok.status, 201);
  assert.equal(ok.body.verificada, true);
  const dup = await registrarEmpresa('nueva-2');
  assert.equal(dup.status, 409);
  const sinEmpresa = await request(app)
    .post('/api/reservas')
    .set(AUTH('sin-empresa'))
    .send({ equipoId: 'eq-demo-1', fechaInicio: '2026-10-20', fechaFin: '2026-10-21' });
  assert.equal(sinEmpresa.status, 422);
  assert.equal(sinEmpresa.body.error.codigo, 'EMPRESA_NO_VERIFICADA');
});

test('CRUD de equipos con foto validada automáticamente (RN-04)', async () => {
  await registrarEmpresa('duena');
  const rechazada = await request(app).post('/api/equipos/fotos').set(AUTH('duena')).attach('foto', fixture('pequena.png'));
  assert.equal(rechazada.status, 422);
  assert.equal(rechazada.body.error.codigo, 'FOTO_RECHAZADA');
  assert.ok(rechazada.body.error.detalles[0].includes('Resolución insuficiente'));
  const alargada = await request(app).post('/api/equipos/fotos').set(AUTH('duena')).attach('foto', fixture('panoramica.png'));
  assert.equal(alargada.status, 422);
  const noImagen = await request(app).post('/api/equipos/fotos').set(AUTH('duena')).attach('foto', fixture('no_imagen.txt'));
  assert.equal(noImagen.status, 422);
  const foto = await request(app).post('/api/equipos/fotos').set(AUTH('duena')).attach('foto', fixture('valida.jpg'));
  assert.equal(foto.status, 201);

  const datos = { nombre: 'Cortadora de cerámica', categoria: 'HERRAMIENTAS', descripcion: 'Cortadora eléctrica con disco diamantado.', precioDia: 35, lat: -8.11, lng: -79.03, direccion: 'Centro de Trujillo' };
  const invalido = await request(app).post('/api/equipos').set(AUTH('duena')).send({ ...datos, precioDia: 0 });
  assert.equal(invalido.status, 400);
  const sinFoto = await request(app).post('/api/equipos').set(AUTH('duena')).send(datos);
  assert.equal(sinFoto.status, 201);
  assert.equal(sinFoto.body.estado, 'PENDIENTE_FOTO');
  const creado = await request(app).post('/api/equipos').set(AUTH('duena')).send({ ...datos, fotoId: foto.body.id });
  assert.equal(creado.status, 201);
  assert.equal(creado.body.estado, 'PUBLICADO');
  assert.ok(creado.body.fotoUrl.endsWith('.jpg'));

  const ajeno = await request(app).put(`/api/equipos/${creado.body.id}`).set(AUTH('demo-taller')).send({ precioDia: 10 });
  assert.equal(ajeno.status, 403);
  const editado = await request(app).put(`/api/equipos/${creado.body.id}`).set(AUTH('duena')).send({ precioDia: 40, visible: false });
  assert.equal(editado.status, 200);
  assert.equal(editado.body.precioDia, 40);
  assert.equal(editado.body.estado, 'OCULTO');
  const mios = await request(app).get('/api/equipos/mios').set(AUTH('duena'));
  assert.equal(mios.body.total, 2);
  const borrado = await request(app).delete(`/api/equipos/${creado.body.id}`).set(AUTH('duena'));
  assert.equal(borrado.status, 204);
  const despues = await request(app).get(`/api/equipos/${creado.body.id}`);
  assert.equal(despues.status, 404);
});

test('Reservas: cotización, anticipación (RN-07), solapamiento (RN-02) y flujo de estados', async () => {
  const cot = await request(app).post('/api/reservas/cotizacion').set(AUTH('demo-eventos')).send({ equipoId: 'eq-demo-1', fechaInicio: '2026-10-18', fechaFin: '2026-10-20' });
  assert.equal(cot.status, 200);
  assert.equal(cot.body.subtotal, 240);
  assert.equal(cot.body.comision, 28.8);
  assert.equal(cot.body.disponible, true);

  const pronto = await request(app).post('/api/reservas').set(AUTH('demo-eventos')).send({ equipoId: 'eq-demo-1', fechaInicio: '2026-10-17', fechaFin: '2026-10-17' });
  assert.equal(pronto.status, 422);
  assert.equal(pronto.body.error.codigo, 'FECHAS_NO_VALIDAS');

  const propia = await request(app).post('/api/reservas').set(AUTH('demo-constructora')).send({ equipoId: 'eq-demo-1', fechaInicio: '2026-10-18', fechaFin: '2026-10-19' });
  assert.equal(propia.status, 422);

  const r1 = await request(app).post('/api/reservas').set(AUTH('demo-eventos')).send({ equipoId: 'eq-demo-1', fechaInicio: '2026-10-18', fechaFin: '2026-10-20' });
  assert.equal(r1.status, 201);
  assert.equal(r1.body.estado, 'PENDIENTE');
  assert.equal(r1.body.total, 240);

  const cruce = await request(app).post('/api/reservas').set(AUTH('demo-taller')).send({ equipoId: 'eq-demo-1', fechaInicio: '2026-10-20', fechaFin: '2026-10-21' });
  assert.equal(cruce.status, 409);
  assert.equal(cruce.body.error.codigo, 'RESERVA_SOLAPADA');

  const disp = await request(app).get('/api/equipos/eq-demo-1/disponibilidad');
  assert.deepEqual(disp.body.ocupados, [{ fechaInicio: '2026-10-18', fechaFin: '2026-10-20' }]);

  const noDueno = await request(app).patch(`/api/reservas/${r1.body.id}/estado`).set(AUTH('demo-eventos')).send({ estado: 'CONFIRMADA' });
  assert.equal(noDueno.status, 403);
  const saltar = await request(app).patch(`/api/reservas/${r1.body.id}/estado`).set(AUTH('demo-constructora')).send({ estado: 'DEVUELTA' });
  assert.equal(saltar.status, 422);
  for (const estado of ['CONFIRMADA', 'EN_CURSO', 'DEVUELTA']) {
    const paso = await request(app).patch(`/api/reservas/${r1.body.id}/estado`).set(AUTH('demo-constructora')).send({ estado });
    assert.equal(paso.status, 200);
    assert.equal(paso.body.estado, estado);
  }
  const mias = await request(app).get('/api/reservas/mias?rol=arrendador').set(AUTH('demo-constructora'));
  assert.ok(mias.body.items.some((x) => x.id === r1.body.id));
  const ajena = await request(app).get(`/api/reservas/${r1.body.id}`).set(AUTH('demo-taller'));
  assert.equal(ajena.status, 403);
});

test('RN-08: cancelación tardía genera 20 % de penalidad; temprana no', async () => {
  const a = await request(app).post('/api/reservas').set(AUTH('demo-eventos')).send({ equipoId: 'eq-demo-2', fechaInicio: '2026-10-18', fechaFin: '2026-10-18' });
  const b = await request(app).post('/api/reservas').set(AUTH('demo-eventos')).send({ equipoId: 'eq-demo-3', fechaInicio: '2026-10-25', fechaFin: '2026-10-26' });
  ahoraMs = Date.parse('2026-10-18T05:00:00Z'); // 8 h antes de la entrega de "a"
  const tardia = await request(app).patch(`/api/reservas/${a.body.id}/cancelar`).set(AUTH('demo-eventos'));
  assert.equal(tardia.status, 200);
  assert.equal(tardia.body.estado, 'CANCELADA');
  assert.equal(tardia.body.penalidad, 18); // 20 % de S/ 90
  const temprana = await request(app).patch(`/api/reservas/${b.body.id}/cancelar`).set(AUTH('demo-eventos'));
  assert.equal(temprana.body.penalidad, 0);
  const otraVez = await request(app).patch(`/api/reservas/${b.body.id}/cancelar`).set(AUTH('demo-eventos'));
  assert.equal(otraVez.status, 422);
});

test('RN-05 y RN-06: reseñas dentro de 48 h y revisión automática tras 5 reseñas bajas', async () => {
  const empresas = ['cli-1', 'cli-2', 'cli-3', 'cli-4', 'cli-5'];
  const rucs = ['20600000005', '20600000013', '20600000021', '20600000030', '20131312955'];
  for (let i = 0; i < empresas.length; i++) {
    const res = await request(app).put('/api/empresas/me').set(AUTH(empresas[i])).send({ ruc: rucs[i], razonSocial: `Cliente ${i}`, telefono: '912345678' });
    assert.equal(res.status, 201, JSON.stringify(res.body));
  }
  let inicio = 18;
  for (const uid of empresas) {
    ahoraMs = Date.parse('2026-10-16T17:00:00Z');
    const fi = `2026-10-${String(inicio).padStart(2, '0')}`;
    const r = await request(app).post('/api/reservas').set(AUTH(uid)).send({ equipoId: 'eq-demo-10', fechaInicio: fi, fechaFin: fi });
    assert.equal(r.status, 201, JSON.stringify(r.body));
    for (const estado of ['CONFIRMADA', 'EN_CURSO', 'DEVUELTA']) {
      await request(app).patch(`/api/reservas/${r.body.id}/estado`).set(AUTH('demo-taller')).send({ estado });
    }
    const resena = await request(app).post(`/api/reservas/${r.body.id}/resenas`).set(AUTH(uid)).send({ estrellas: 2, comentario: 'Llegó con fallas.' });
    assert.equal(resena.status, 201);
    const duplicada = await request(app).post(`/api/reservas/${r.body.id}/resenas`).set(AUTH(uid)).send({ estrellas: 2, comentario: 'Otra vez.' });
    assert.equal(duplicada.status, 409);
    inicio += 1;
  }
  const equipo = await request(app).get('/api/equipos/eq-demo-10');
  assert.equal(equipo.body.totalResenas, 5);
  assert.equal(equipo.body.calificacionPromedio, 2);
  assert.equal(equipo.body.estado, 'EN_REVISION');
  const catalogo = await request(app).get('/api/equipos?q=escalera');
  assert.equal(catalogo.body.total, 0);

  // Fuera de plazo (más de 48 h tras la devolución)
  const r = await request(app).get('/api/reservas/mias?rol=arrendador').set(AUTH('demo-taller'));
  const devuelta = r.body.items[0];
  ahoraMs += 49 * 3_600_000;
  const tarde = await request(app).post(`/api/reservas/${devuelta.id}/resenas`).set(AUTH('demo-taller')).send({ estrellas: 4, comentario: 'Buen cliente.' });
  assert.equal(tarde.status, 422);
  assert.equal(tarde.body.error.codigo, 'RESENA_FUERA_DE_PLAZO');
});

test('No se elimina un equipo con reservas activas', async () => {
  await request(app).post('/api/reservas').set(AUTH('demo-eventos')).send({ equipoId: 'eq-demo-8', fechaInicio: '2026-10-19', fechaFin: '2026-10-19' });
  const res = await request(app).delete('/api/equipos/eq-demo-8').set(AUTH('demo-taller'));
  assert.equal(res.status, 409);
  assert.equal(res.body.error.codigo, 'EQUIPO_CON_RESERVAS');
});

test('Modo Firebase: rechaza tokens inválidos y acepta los verificados', async () => {
  const store = crearStore({ datosIniciales: datosDemo(null) });
  const appFb = crearApp({
    store,
    carpetaUploads: os.tmpdir(),
    firebaseProjectId: 'equishare-test',
    registrarPeticiones: false,
    verificador: async (token) => {
      if (token !== 'token-valido') throw new Error('firma inválida');
      return { uid: 'demo-eventos', email: 'eventos@demo.pe' };
    },
  });
  const salud = await request(appFb).get('/api/health');
  assert.equal(salud.body.modoAutenticacion, 'firebase');
  const malo = await request(appFb).get('/api/empresas/me').set('Authorization', 'Bearer demo:demo-eventos');
  assert.equal(malo.status, 401);
  const bueno = await request(appFb).get('/api/empresas/me').set('Authorization', 'Bearer token-valido');
  assert.equal(bueno.status, 200);
  assert.equal(bueno.body.id, 'demo-eventos');
});
