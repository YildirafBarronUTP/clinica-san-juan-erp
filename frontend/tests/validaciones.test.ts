import { edad, esDniValido, esTelefonoValido, fechaIso, validarPaciente } from '@/lib/validaciones';

const base = {
  dni: '45678912', nombres: 'Ana', apellidos: 'Pérez', fechaNacimiento: '1990-05-01', sexo: 'F',
  telefono: '', email: '', direccion: '',
};
const HOY = new Date(2026, 9, 13); // 13/10/2026 hora local

describe('CP-FE-001 Validación de DNI (valores límite)', () => {
  it.each(['12345678', '00000001'])('%s es válido', (d) => expect(esDniValido(d)).toBe(true));
  it.each(['1234567', '123456789', '1234567a', '', ' 1234567'])('"%s" es inválido', (d) => expect(esDniValido(d)).toBe(false));
});

describe('CP-FE-002 Teléfono opcional de 9 dígitos que empieza con 9', () => {
  it.each(['', '987654321'])('"%s" es válido', (t) => expect(esTelefonoValido(t)).toBe(true));
  it.each(['87654321', '9876543210', '887654321'])('"%s" es inválido', (t) => expect(esTelefonoValido(t)).toBe(false));
});

describe('CP-FE-003 Formulario de paciente', () => {
  it('un formulario completo no tiene errores', () => {
    expect(validarPaciente(base, HOY)).toEqual({});
  });
  it('marca todos los obligatorios vacíos', () => {
    const e = validarPaciente({ ...base, dni: '', nombres: ' ', apellidos: '', fechaNacimiento: '', sexo: '' }, HOY);
    expect(Object.keys(e).sort()).toEqual(['apellidos', 'dni', 'fechaNacimiento', 'nombres', 'sexo']);
  });
  it('fecha de nacimiento: hoy es válida, mañana no', () => {
    expect(validarPaciente({ ...base, fechaNacimiento: '2026-10-13' }, HOY)).toEqual({});
    expect(validarPaciente({ ...base, fechaNacimiento: '2026-10-14' }, HOY).fechaNacimiento).toMatch(/futura/);
  });
  it('correo inválido', () => {
    expect(validarPaciente({ ...base, email: 'ana@' }, HOY).email).toBeDefined();
  });
});

describe('CP-FE-004 Utilidades de fecha', () => {
  it('fechaIso usa la fecha local', () => expect(fechaIso(new Date(2026, 0, 5, 23, 30))).toBe('2026-01-05'));
  it('edad cumple años exactamente en su fecha', () => {
    expect(edad('2000-10-13', HOY)).toBe(26);
    expect(edad('2000-10-14', HOY)).toBe(25);
  });
});
