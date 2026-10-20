// Autenticación con Firebase Authentication.
// - Modo "firebase" (FIREBASE_PROJECT_ID definido): verifica la firma del ID token con las claves
//   públicas de Google y valida emisor, audiencia y vigencia, como indica la documentación de
//   Firebase para bibliotecas JWT de terceros (no requiere cuenta de servicio).
// - Modo "demo" (sin FIREBASE_PROJECT_ID): solo para desarrollo local. Acepta "demo:<uid>".
import { createRemoteJWKSet, jwtVerify, decodeJwt } from 'jose';
import { errores } from '../errors.js';

const JWKS_URL =
  'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com';

export function crearAutenticador({ firebaseProjectId = null, verificador = null } = {}) {
  const modo = firebaseProjectId ? 'firebase' : 'demo';
  let jwks = null;

  async function verificarFirebase(token) {
    if (verificador) return verificador(token); // inyectable en pruebas
    jwks ??= createRemoteJWKSet(new URL(JWKS_URL));
    const { payload } = await jwtVerify(token, jwks, {
      issuer: `https://securetoken.google.com/${firebaseProjectId}`,
      audience: firebaseProjectId,
      algorithms: ['RS256'],
    });
    if (!payload.sub) throw new Error('Token sin sujeto');
    return { uid: payload.sub, email: payload.email ?? null };
  }

  function usuarioDemo(token) {
    if (token.startsWith('demo:')) {
      const uid = token.slice(5).trim();
      if (!/^[\w-]{3,64}$/.test(uid)) throw new Error('uid demo inválido');
      return { uid, email: `${uid}@demo.equishare` };
    }
    // Token de Firebase usado con el servidor en modo demo: se lee sin verificar firma.
    const payload = decodeJwt(token);
    const uid = payload.user_id ?? payload.sub;
    if (!uid) throw new Error('Token sin uid');
    return { uid, email: payload.email ?? null };
  }

  async function autenticar(req, _res, next) {
    const cabecera = req.get('authorization') ?? '';
    const [tipo, token] = cabecera.split(' ');
    if (tipo !== 'Bearer' || !token) return next(errores.noAutenticado());
    try {
      req.usuario = modo === 'firebase' ? await verificarFirebase(token) : usuarioDemo(token);
      return next();
    } catch {
      return next(errores.noAutenticado('La sesión no es válida o expiró. Vuelve a iniciar sesión.'));
    }
  }

  return { modo, autenticar };
}
