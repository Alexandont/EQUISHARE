// Almacén de datos en archivo JSON con escritura atómica (sin dependencias nativas ni costo).
// Para producción se reemplazaría por una base de datos gestionada; el resto del código solo
// depende de las funciones que expone este módulo (patrón Repository en el servidor).
import fs from 'node:fs';
import path from 'node:path';

const COLECCIONES = ['empresas', 'equipos', 'reservas', 'resenas', 'fotos'];

function vacio() {
  return Object.fromEntries(COLECCIONES.map((c) => [c, []]));
}

export function crearStore({ archivo = null, datosIniciales = null } = {}) {
  let db = vacio();
  if (archivo && fs.existsSync(archivo)) {
    db = { ...vacio(), ...JSON.parse(fs.readFileSync(archivo, 'utf8')) };
  } else if (datosIniciales) {
    db = { ...vacio(), ...structuredClone(datosIniciales) };
  }

  function guardar() {
    if (!archivo) return;
    fs.mkdirSync(path.dirname(archivo), { recursive: true });
    const tmp = `${archivo}.tmp`;
    fs.writeFileSync(tmp, JSON.stringify(db, null, 2));
    fs.renameSync(tmp, archivo);
  }

  return {
    listar(coleccion, filtro = () => true) {
      return db[coleccion].filter(filtro);
    },
    obtener(coleccion, id) {
      return db[coleccion].find((x) => x.id === id) ?? null;
    },
    insertar(coleccion, item) {
      db[coleccion].push(item);
      guardar();
      return item;
    },
    actualizar(coleccion, id, cambios) {
      const item = db[coleccion].find((x) => x.id === id);
      if (!item) return null;
      Object.assign(item, cambios);
      guardar();
      return item;
    },
    eliminar(coleccion, id) {
      const antes = db[coleccion].length;
      db[coleccion] = db[coleccion].filter((x) => x.id !== id);
      guardar();
      return db[coleccion].length < antes;
    },
    guardar,
  };
}
