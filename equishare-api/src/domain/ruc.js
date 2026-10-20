// Validación estructural del RUC peruano (11 dígitos, prefijo válido y dígito verificador
// módulo 11).
const PESOS = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];
const PREFIJOS_VALIDOS = ['10', '15', '16', '17', '20'];

export function digitoVerificadorRuc(primeros10) {
  const suma = primeros10
    .split('')
    .reduce((acc, d, i) => acc + Number(d) * PESOS[i], 0);
  const resultado = 11 - (suma % 11);
  if (resultado === 10) return 0;
  if (resultado === 11) return 1;
  return resultado;
}

export function validarRuc(ruc) {
  const valor = String(ruc ?? '').trim();
  if (!/^\d{11}$/.test(valor)) {
    return { valido: false, motivo: 'El RUC debe tener exactamente 11 dígitos numéricos.' };
  }
  if (!PREFIJOS_VALIDOS.includes(valor.slice(0, 2))) {
    return { valido: false, motivo: 'El RUC debe empezar con 10, 15, 16, 17 o 20.' };
  }
  const esperado = digitoVerificadorRuc(valor.slice(0, 10));
  if (esperado !== Number(valor[10])) {
    return { valido: false, motivo: 'El dígito verificador del RUC no es correcto.' };
  }
  return { valido: true, motivo: null };
}
