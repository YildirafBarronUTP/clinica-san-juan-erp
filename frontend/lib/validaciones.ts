// Validaciones de cliente. Repiten las reglas del backend para dar feedback inmediato;
// el backend sigue siendo la fuente de verdad.

export type Errores = Record<string, string>;

export const esDniValido = (dni: string) => /^\d{8}$/.test(dni);

export const esTelefonoValido = (tel: string) => tel === '' || /^9\d{8}$/.test(tel);

export const esEmailValido = (email: string) => email === '' || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

export interface FormPaciente {
  dni: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento: string;
  sexo: string;
  telefono: string;
  email: string;
  direccion: string;
}

export function validarPaciente(f: FormPaciente, hoy: Date = new Date()): Errores {
  const e: Errores = {};
  if (!esDniValido(f.dni.trim())) e.dni = 'El DNI debe tener exactamente 8 dígitos';
  if (!f.nombres.trim()) e.nombres = 'Ingrese los nombres';
  if (!f.apellidos.trim()) e.apellidos = 'Ingrese los apellidos';
  if (!f.fechaNacimiento) {
    e.fechaNacimiento = 'Ingrese la fecha de nacimiento';
  } else if (f.fechaNacimiento > fechaIso(hoy)) {
    e.fechaNacimiento = 'La fecha de nacimiento no puede ser futura';
  }
  if (f.sexo !== 'M' && f.sexo !== 'F') e.sexo = 'Seleccione el sexo';
  if (!esTelefonoValido(f.telefono.trim())) e.telefono = 'El teléfono debe tener 9 dígitos y empezar con 9';
  if (!esEmailValido(f.email.trim())) e.email = 'Correo electrónico inválido';
  return e;
}

/** Fecha local en formato YYYY-MM-DD (evita el desfase de toISOString en Lima, UTC-5). */
export function fechaIso(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

export function edad(fechaNacimiento: string, hoy: Date = new Date()): number {
  const [a, m, d] = fechaNacimiento.split('-').map(Number);
  let años = hoy.getFullYear() - a;
  if (hoy.getMonth() + 1 < m || (hoy.getMonth() + 1 === m && hoy.getDate() < d)) años--;
  return años;
}

export const soles = (v: number | string) =>
  'S/ ' + Number(v).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
