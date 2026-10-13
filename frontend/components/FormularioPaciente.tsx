'use client';

import { FormEvent, useState } from 'react';
import { ApiError } from '@/lib/api';
import { Errores, FormPaciente, validarPaciente } from '@/lib/validaciones';
import { Campo } from './Campo';

export const FORM_VACIO: FormPaciente = {
  dni: '', nombres: '', apellidos: '', fechaNacimiento: '', sexo: '', telefono: '', email: '', direccion: '',
};

export function FormularioPaciente({ inicial, textoBoton, onGuardar }: {
  inicial: FormPaciente;
  textoBoton: string;
  onGuardar: (f: FormPaciente) => Promise<void>;
}) {
  const [f, setF] = useState<FormPaciente>(inicial);
  const [errores, setErrores] = useState<Errores>({});
  const [general, setGeneral] = useState('');
  const [guardando, setGuardando] = useState(false);

  const set = (k: keyof FormPaciente) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setF({ ...f, [k]: e.target.value });

  async function enviar(e: FormEvent) {
    e.preventDefault();
    const errs = validarPaciente(f);
    setErrores(errs);
    setGeneral('');
    if (Object.keys(errs).length) return;
    setGuardando(true);
    try {
      await onGuardar({ ...f, dni: f.dni.trim(), telefono: f.telefono.trim(), email: f.email.trim() });
    } catch (err) {
      if (err instanceof ApiError) {
        setErrores(err.porCampo());
        setGeneral(err.message);
      } else {
        setGeneral('Error inesperado');
      }
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="card" onSubmit={enviar} noValidate data-testid="form-paciente">
      {general && <div className="alerta bad" role="alert" data-testid="form-error">{general}</div>}
      <div className="grid">
        <Campo etiqueta="DNI" nombre="dni" value={f.dni} onChange={set('dni')} error={errores.dni} inputMode="numeric" maxLength={8} />
        <Campo etiqueta="Nombres" nombre="nombres" value={f.nombres} onChange={set('nombres')} error={errores.nombres} />
        <Campo etiqueta="Apellidos" nombre="apellidos" value={f.apellidos} onChange={set('apellidos')} error={errores.apellidos} />
        <Campo etiqueta="Fecha de nacimiento" nombre="fechaNacimiento" type="date" value={f.fechaNacimiento}
               onChange={set('fechaNacimiento')} error={errores.fechaNacimiento} />
        <div className="campo">
          <label htmlFor="sexo">Sexo</label>
          <select id="sexo" value={f.sexo} onChange={set('sexo')} aria-invalid={errores.sexo ? 'true' : 'false'}>
            <option value="">Seleccione</option>
            <option value="F">Femenino</option>
            <option value="M">Masculino</option>
          </select>
          {errores.sexo && <div className="error" data-testid="error-sexo">{errores.sexo}</div>}
        </div>
        <Campo etiqueta="Teléfono (opcional)" nombre="telefono" value={f.telefono} onChange={set('telefono')}
               error={errores.telefono} inputMode="numeric" maxLength={9} />
        <Campo etiqueta="Correo (opcional)" nombre="email" type="email" value={f.email} onChange={set('email')} error={errores.email} />
        <Campo etiqueta="Dirección (opcional)" nombre="direccion" value={f.direccion} onChange={set('direccion')} error={errores.direccion} />
      </div>
      <button type="submit" disabled={guardando} data-testid="guardar-paciente">{guardando ? 'Guardando…' : textoBoton}</button>
    </form>
  );
}
