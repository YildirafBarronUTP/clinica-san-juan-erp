'use client';

import { FormEvent, Suspense, useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { api, ApiError } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { PacienteDetalle } from '@/lib/tipos';
import { edad } from '@/lib/validaciones';
import { EstadoChip } from '@/components/EstadoChip';
import { FormularioPaciente } from '@/components/FormularioPaciente';

function Ficha() {
  const params = useSearchParams();
  const id = params.get('id');
  const { sesion } = useAuth();
  const [p, setP] = useState<PacienteDetalle | null>(null);
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState(params.get('nuevo') ? 'Paciente registrado correctamente' : '');
  const [editando, setEditando] = useState(false);
  const [alergia, setAlergia] = useState('');
  const [reaccion, setReaccion] = useState('');

  const cargar = useCallback(async () => {
    try {
      setP(await api<PacienteDetalle>(`/pacientes/${id}`));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error');
    }
  }, [id]);

  useEffect(() => { cargar(); }, [cargar]);

  const puedeEditar = sesion && ['ADMIN', 'ADMISION'].includes(sesion.rol) && p?.estado !== 'FALLECIDO';

  async function cambiarEstado(estado: string) {
    if (estado === 'FALLECIDO' && !confirm('Marcar como FALLECIDO es irreversible. ¿Continuar?')) return;
    try {
      setP(await api<PacienteDetalle>(`/pacientes/${id}/estado`, { method: 'PATCH', json: { estado } }));
      setMensaje(`Estado cambiado a ${estado}`);
      setError('');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error');
    }
  }

  async function agregarAlergia(e: FormEvent) {
    e.preventDefault();
    try {
      await api(`/pacientes/${id}/alergias`, { method: 'POST', json: { principioActivo: alergia, reaccion } });
      setAlergia(''); setReaccion(''); setError('');
      await cargar();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Error');
    }
  }

  if (!p) return error ? <div className="alerta bad">{error}</div> : <p>Cargando…</p>;

  return (
    <>
      <Link href="/pacientes/">← Volver a pacientes</Link>
      <div className="fila entre" style={{ marginTop: 10 }}>
        <div>
          <h1 data-testid="ficha-nombre">{p.apellidos}, {p.nombres}</h1>
          <p className="sub">{p.historiaClinica} · DNI {p.dni} · {edad(p.fechaNacimiento)} años · <EstadoChip estado={p.estado} /></p>
        </div>
        {puedeEditar && !editando && <button className="sec" onClick={() => setEditando(true)}>Editar datos</button>}
      </div>
      {mensaje && <div className="alerta ok" data-testid="ficha-mensaje">{mensaje}</div>}
      {error && <div className="alerta bad" role="alert">{error}</div>}

      {editando ? (
        <FormularioPaciente
          inicial={{ dni: p.dni, nombres: p.nombres, apellidos: p.apellidos, fechaNacimiento: p.fechaNacimiento, sexo: p.sexo,
                     telefono: p.telefono ?? '', email: p.email ?? '', direccion: p.direccion ?? '' }}
          textoBoton="Guardar cambios"
          onGuardar={async (f) => {
            setP(await api<PacienteDetalle>(`/pacientes/${id}`, { method: 'PUT', json: f }));
            setEditando(false);
            setMensaje('Datos actualizados');
          }} />
      ) : (
        <div className="card">
          <h2>Datos personales</h2>
          <div className="grid">
            <div><label>Sexo</label>{p.sexo === 'F' ? 'Femenino' : 'Masculino'}</div>
            <div><label>Nacimiento</label>{p.fechaNacimiento}</div>
            <div><label>Teléfono</label>{p.telefono ?? '—'}</div>
            <div><label>Correo</label>{p.email ?? '—'}</div>
            <div><label>Dirección</label>{p.direccion ?? '—'}</div>
          </div>
        </div>
      )}

      <div className="card">
        <h2>Alergias</h2>
        {p.alergias.length === 0 ? <p className="sub">Sin alergias registradas</p> : (
          <ul data-testid="lista-alergias">
            {p.alergias.map((a) => <li key={a.id}><strong>{a.principioActivo}</strong>{a.reaccion ? ` — ${a.reaccion}` : ''}</li>)}
          </ul>
        )}
        {p.estado !== 'FALLECIDO' && (
          <form className="fila" onSubmit={agregarAlergia} style={{ alignItems: 'flex-end' }}>
            <div style={{ flex: 1, minWidth: 180 }}>
              <label htmlFor="alergia">Principio activo</label>
              <input id="alergia" value={alergia} onChange={(e) => setAlergia(e.target.value)} required />
            </div>
            <div style={{ flex: 2, minWidth: 180 }}>
              <label htmlFor="reaccion">Reacción (opcional)</label>
              <input id="reaccion" value={reaccion} onChange={(e) => setReaccion(e.target.value)} />
            </div>
            <button type="submit" className="sec">Agregar alergia</button>
          </form>
        )}
      </div>

      {puedeEditar && p.estadosPermitidos.length > 0 && (
        <div className="card">
          <h2>Estado del expediente</h2>
          <div className="fila">
            {p.estadosPermitidos.map((e) => (
              <button key={e} className={e === 'FALLECIDO' ? 'peligro' : 'sec'} onClick={() => cambiarEstado(e)}
                      data-testid={`estado-${e}`}>Cambiar a {e}</button>
            ))}
          </div>
        </div>
      )}
    </>
  );
}

export default function FichaPage() {
  return <Suspense><Ficha /></Suspense>;
}
