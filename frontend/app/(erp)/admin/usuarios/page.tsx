'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import { api, ApiError } from '@/lib/api';
import { NOMBRE_ROL, Rol } from '@/lib/roles';
import { Usuario } from '@/lib/tipos';
import { Campo } from '@/components/Campo';

const VACIO = { username: '', password: '', nombreCompleto: '', rol: 'ADMISION' };

export default function Usuarios() {
  const [lista, setLista] = useState<Usuario[]>([]);
  const [f, setF] = useState(VACIO);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState('');

  const cargar = useCallback(() => api<Usuario[]>('/usuarios').then(setLista).catch((e) => setError(e.message)), []);
  useEffect(() => { cargar(); }, [cargar]);

  async function crear(e: FormEvent) {
    e.preventDefault();
    try {
      await api('/usuarios', { method: 'POST', json: f });
      setF(VACIO); setErrores({}); setError(''); setMensaje('Usuario creado');
      cargar();
    } catch (err) {
      if (err instanceof ApiError) { setErrores(err.porCampo()); setError(err.message); setMensaje(''); }
    }
  }

  async function alternar(u: Usuario) {
    try {
      await api(`/usuarios/${u.id}/estado`, { method: 'PATCH', json: { activo: !u.activo } });
      setError('');
      cargar();
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  return (
    <>
      <h1>Usuarios y roles</h1>
      <p className="sub">Solo el administrador gestiona accesos</p>
      {mensaje && <div className="alerta ok">{mensaje}</div>}
      {error && <div className="alerta bad" role="alert">{error}</div>}
      <form className="card" onSubmit={crear} noValidate>
        <h2>Nuevo usuario</h2>
        <div className="grid">
          <Campo etiqueta="Usuario" nombre="username" value={f.username} onChange={(e) => setF({ ...f, username: e.target.value })} error={errores.username} />
          <Campo etiqueta="Nombre completo" nombre="nombreCompleto" value={f.nombreCompleto} onChange={(e) => setF({ ...f, nombreCompleto: e.target.value })} error={errores.nombreCompleto} />
          <Campo etiqueta="Contraseña" nombre="password" type="password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} error={errores.password} />
          <div className="campo">
            <label htmlFor="rol">Rol</label>
            <select id="rol" value={f.rol} onChange={(e) => setF({ ...f, rol: e.target.value })}>
              {(Object.keys(NOMBRE_ROL) as Rol[]).map((r) => <option key={r} value={r}>{NOMBRE_ROL[r]}</option>)}
            </select>
          </div>
        </div>
        <button type="submit">Crear usuario</button>
      </form>
      <div className="tabla"><table data-testid="tabla-usuarios">
        <thead><tr><th>Usuario</th><th>Nombre</th><th>Rol</th><th>Estado</th><th></th></tr></thead>
        <tbody>{lista.map((u) => (
          <tr key={u.id}>
            <td>{u.username}</td><td>{u.nombreCompleto}</td><td>{NOMBRE_ROL[u.rol as Rol]}</td>
            <td>{u.activo ? <span className="chip ok">Activo</span> : <span className="chip neutro">Inactivo</span>}</td>
            <td><button className="sec" onClick={() => alternar(u)}>{u.activo ? 'Desactivar' : 'Activar'}</button></td>
          </tr>))}
        </tbody>
      </table></div>
    </>
  );
}
