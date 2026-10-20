import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validarRuc } from '../src/domain/ruc.js';
import {
  cotizar,
  validarFechasReserva,
  haySolapamiento,
  calcularPenalidad,
  verificarVentanaResena,
  requiereRevision,
  diasAlquiler,
  distanciaKm,
} from '../src/domain/reglas.js';

const AHORA = new Date('2026-10-16T17:00:00Z'); // 12:00 en Lima

test('RUC: acepta RUC con dígito verificador correcto', () => {
  assert.equal(validarRuc('20131312955').valido, true);
  assert.equal(validarRuc('20100047218').valido, true);
});

test('RUC: rechaza longitud, prefijo y dígito verificador incorrectos', () => {
  assert.equal(validarRuc('2013131295').valido, false);
  assert.equal(validarRuc('30131312955').valido, false);
  assert.equal(validarRuc('20131312954').valido, false);
  assert.equal(validarRuc('abc').valido, false);
});

test('RN-03: cotización con comisión del 12 % descontada al arrendador', () => {
  const c = cotizar(80, '2026-10-18', '2026-10-20');
  assert.deepEqual(c, { dias: 3, precioDia: 80, subtotal: 240, comision: 28.8, montoArrendador: 211.2, total: 240 });
});

test('Días de alquiler incluyen ambas fechas', () => {
  assert.equal(diasAlquiler('2026-10-18', '2026-10-18'), 1);
  assert.equal(diasAlquiler('2026-10-30', '2026-11-02'), 4);
});

test('RN-07: exige 24 horas de anticipación respecto a la entrega de las 08:00', () => {
  assert.ok(validarFechasReserva('2026-10-17', '2026-10-17', AHORA).length > 0); // 20 h
  assert.deepEqual(validarFechasReserva('2026-10-18', '2026-10-19', AHORA), []); // 44 h
});

test('Fechas: formato y orden', () => {
  assert.ok(validarFechasReserva('18/10/2026', '2026-10-19', AHORA).length > 0);
  assert.ok(validarFechasReserva('2026-10-20', '2026-10-19', AHORA).length > 0);
  assert.ok(validarFechasReserva('2026-02-30', '2026-03-01', AHORA).length > 0);
});

test('RN-02: detecta solapamiento solo con reservas activas', () => {
  const reservas = [
    { id: 'a', estado: 'CONFIRMADA', fechaInicio: '2026-10-20', fechaFin: '2026-10-22' },
    { id: 'b', estado: 'CANCELADA', fechaInicio: '2026-10-25', fechaFin: '2026-10-26' },
  ];
  assert.equal(haySolapamiento(reservas, '2026-10-22', '2026-10-23'), true);
  assert.equal(haySolapamiento(reservas, '2026-10-23', '2026-10-24'), false);
  assert.equal(haySolapamiento(reservas, '2026-10-25', '2026-10-25'), false);
});

test('RN-08: penalidad del 20 % si se cancela con menos de 12 horas', () => {
  const reserva = { fechaInicio: '2026-10-18', total: 240 };
  assert.equal(calcularPenalidad(reserva, new Date('2026-10-17T12:00:00Z')), 0); // 25 h antes
  assert.equal(calcularPenalidad(reserva, new Date('2026-10-18T05:00:00Z')), 48); // 8 h antes
});

test('RN-05: ventana de 48 horas para calificar', () => {
  const r = { estado: 'DEVUELTA', devueltaEn: '2026-10-10T12:00:00Z' };
  assert.equal(verificarVentanaResena(r, new Date('2026-10-11T12:00:00Z')).permitido, true);
  assert.equal(verificarVentanaResena(r, new Date('2026-10-13T12:00:01Z')).permitido, false);
  assert.equal(verificarVentanaResena({ estado: 'EN_CURSO' }, AHORA).permitido, false);
});

test('RN-06: revisión con promedio < 3.0 y al menos 5 reseñas', () => {
  assert.equal(requiereRevision(2.8, 5), true);
  assert.equal(requiereRevision(2.8, 4), false);
  assert.equal(requiereRevision(3.0, 8), false);
});

test('Distancia haversine Trujillo centro - Huanchaco (~10 km)', () => {
  const d = distanciaKm(-8.1116, -79.0287, -8.0792, -79.1199);
  assert.ok(d > 9 && d < 12, `distancia ${d}`);
});
