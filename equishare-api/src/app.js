import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { ApiError } from './errors.js';
import { crearAutenticador } from './middleware/auth.js';
import { rutasEmpresas } from './routes/empresas.js';
import { rutasEquipos } from './routes/equipos.js';
import { rutasReservas } from './routes/reservas.js';
import { CATEGORIAS } from './domain/validaciones.js';

export function crearApp({
  store,
  carpetaUploads,
  firebaseProjectId = null,
  verificador = null,
  reloj = () => new Date(),
  registrarPeticiones = true,
}) {
  const app = express();
  const { modo, autenticar } = crearAutenticador({ firebaseProjectId, verificador });

  app.disable('x-powered-by');
  app.set('trust proxy', 'loopback');
  app.use(helmet({ crossOriginResourcePolicy: { policy: 'cross-origin' } }));
  app.use(cors());
  app.use(express.json({ limit: '100kb' }));
  if (registrarPeticiones) app.use(morgan('dev'));

  app.get('/api/health', (_req, res) => {
    res.json({ estado: 'ok', servicio: 'equishare-api', modoAutenticacion: modo, hora: reloj().toISOString() });
  });
  app.get('/api/categorias', (_req, res) => res.json(CATEGORIAS));

  app.use('/uploads', express.static(carpetaUploads, { maxAge: '1d', fallthrough: true }));
  app.use('/api/empresas', rutasEmpresas({ store, autenticar, reloj }));
  app.use('/api/equipos', rutasEquipos({ store, autenticar, reloj, carpetaUploads }));
  app.use('/api/reservas', rutasReservas({ store, autenticar, reloj }));

  app.use((req, _res, next) => next(new ApiError(404, 'RUTA_NO_ENCONTRADA', `No existe la ruta ${req.method} ${req.path}.`)));

  // Manejador central de errores: siempre responde JSON con el mismo contrato.
  // eslint-disable-next-line no-unused-vars
  app.use((err, _req, res, _next) => {
    if (err instanceof ApiError) {
      return res.status(err.status).json({ error: { codigo: err.codigo, mensaje: err.message, detalles: err.detalles ?? [] } });
    }
    if (err?.type === 'entity.parse.failed') {
      return res.status(400).json({ error: { codigo: 'JSON_INVALIDO', mensaje: 'El cuerpo de la petición no es un JSON válido.', detalles: [] } });
    }
    if (err?.type === 'entity.too.large') {
      return res.status(413).json({ error: { codigo: 'CUERPO_MUY_GRANDE', mensaje: 'El cuerpo de la petición es demasiado grande.', detalles: [] } });
    }
    console.error(err);
    return res.status(500).json({ error: { codigo: 'ERROR_INTERNO', mensaje: 'Ocurrió un error inesperado en el servidor.', detalles: [] } });
  });

  return app;
}
