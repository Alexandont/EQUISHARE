# equishare-api

API REST de EQUISHARE en Node.js + Express.

```bash
npm install
npm test      # pruebas de reglas de negocio y endpoints (node:test + supertest)
npm start     # http://localhost:3000  (emulador Android: http://10.0.2.2:3000)
npm run seed  # reinicia los datos de demostración
```

* Contrato JSON: `docs/openapi.yaml`.
* Errores siempre con el formato `{ "error": { "codigo", "mensaje", "detalles" } }` y códigos HTTP 400, 401, 403, 404, 409, 413, 422 y 500.
* Autenticación: `Authorization: Bearer <ID token de Firebase>`. Sin `FIREBASE_PROJECT_ID` en `.env` funciona en modo demostración con `Bearer demo:<uid>`.
* Reglas de negocio: `src/domain/reglas.js` (RN-02, RN-03, RN-05 a RN-08) y `src/domain/ruc.js` (RN-01). Validación automática de fotos (RN-04, fase 1 sin ML): `src/services/validacionFoto.js`.

| Método | Ruta | Descripción |
|---|---|---|
| GET | /api/health | Estado del servicio |
| GET/PUT | /api/empresas/me | Perfil de la empresa y verificación del RUC |
| GET | /api/empresas/:id | Perfil público |
| GET | /api/equipos | Catálogo (q, categoria, lat, lng, radioKm) |
| GET | /api/equipos/mios | Equipos de la empresa |
| POST | /api/equipos/fotos | Subida y validación automática de la foto |
| POST/PUT/DELETE | /api/equipos[/:id] | CRUD de equipos |
| GET | /api/equipos/:id/disponibilidad | Fechas ocupadas |
| GET | /api/equipos/:id/resenas | Reseñas del equipo |
| POST | /api/reservas/cotizacion | Cálculo de costos sin reservar |
| POST | /api/reservas | Crear reserva |
| GET | /api/reservas/mias?rol= | Reservas como arrendatario o arrendador |
| PATCH | /api/reservas/:id/cancelar | Cancelar (penalidad RN-08) |
| PATCH | /api/reservas/:id/estado | Confirmar, entregar, devolver |
| POST | /api/reservas/:id/resenas | Calificar (RN-05, RN-06) |
