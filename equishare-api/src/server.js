import path from 'node:path';
import fs from 'node:fs';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import { crearApp } from './app.js';
import { crearStore } from './data/store.js';
import { sembrarSiVacio } from './data/seed.js';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
try {
  process.loadEnvFile(path.join(raiz, '.env'));
} catch {
  // Sin archivo .env: se usan los valores por defecto.
}

const PUERTO = Number(process.env.PORT ?? 3000);
const archivoDatos = path.resolve(raiz, process.env.DATA_FILE ?? 'data/db.json');
const carpetaUploads = path.resolve(raiz, 'uploads');
const firebaseProjectId = process.env.FIREBASE_PROJECT_ID?.trim() || null;

fs.mkdirSync(carpetaUploads, { recursive: true });
const store = crearStore({ archivo: archivoDatos });
sembrarSiVacio(store, carpetaUploads);

const app = crearApp({ store, carpetaUploads, firebaseProjectId });

app.listen(PUERTO, '0.0.0.0', () => {
  const ips = Object.values(os.networkInterfaces())
    .flat()
    .filter((i) => i && i.family === 'IPv4' && !i.internal)
    .map((i) => i.address);
  console.log(`EQUISHARE API escuchando en el puerto ${PUERTO}`);
  console.log(`  Emulador Android:   http://10.0.2.2:${PUERTO}/`);
  ips.forEach((ip) => console.log(`  Celular en la red:  http://${ip}:${PUERTO}/`));
  console.log(`  Autenticación: ${firebaseProjectId ? `Firebase (${firebaseProjectId})` : 'DEMO (sin verificación de tokens)'}`);
});
