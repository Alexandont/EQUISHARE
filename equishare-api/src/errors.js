// Error de la API con formato JSON uniforme: { error: { codigo, mensaje, detalles } }
export class ApiError extends Error {
  constructor(status, codigo, mensaje, detalles = undefined) {
    super(mensaje);
    this.status = status;
    this.codigo = codigo;
    this.detalles = detalles;
  }
}

export const errores = {
  validacion: (detalles, mensaje = 'Los datos enviados no son válidos.') =>
    new ApiError(400, 'VALIDACION', mensaje, detalles),
  noAutenticado: (mensaje = 'Debes iniciar sesión para usar este recurso.') =>
    new ApiError(401, 'NO_AUTENTICADO', mensaje),
  prohibido: (mensaje = 'No tienes permiso para realizar esta acción.') =>
    new ApiError(403, 'PROHIBIDO', mensaje),
  noEncontrado: (recurso) => new ApiError(404, 'NO_ENCONTRADO', `${recurso} no existe.`),
  conflicto: (codigo, mensaje) => new ApiError(409, codigo, mensaje),
  reglaNegocio: (codigo, mensaje, detalles) => new ApiError(422, codigo, mensaje, detalles),
};
