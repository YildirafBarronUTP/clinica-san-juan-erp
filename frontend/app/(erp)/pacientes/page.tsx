'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { api, ApiError, Pagina, qs } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { PacienteResumen } from '@/lib/tipos';
import { edad } from '@/lib/validaciones';
import { Paginador } from '@/components/Paginador';
import { EstadoChip } from '@/components/EstadoChip';

export default function Pacientes() {
  const { sesion } = useAuth();
  const [q, setQ] = useState('');
  const [estado, setEstado] = useState('');
  const [pagina, setPagina] = useState(0);
  const [datos, setDatos] = useState<Pagina<PacienteResumen> | null>(null);
  const [error, setError] = useState('');

  const cargar = useCallback(async (p: number) => {
    try {
      setDatos(await api<Pagina<PacienteResumen>>('/pacientes' + qs({ q, estado, page: p, size: 15 })));
      setError('');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error al cargar');
    }
  }, [q, estado]);

  useEffect(() => {
    const t = setTimeout(() => cargar(pagina), 250);
    return () => clearTimeout(t);
  }, [cargar, pagina]);

  const puedeRegistrar = sesion && ['ADMIN', 'ADMISION'].includes(sesion.rol);

  return (
    <>
      <div className="fila entre">
        <div>
          <h1>Pacientes</h1>
          <p className="sub">Expedientes clínicos registrados</p>
        </div>
        {puedeRegistrar && <Link className="boton" href="/pacientes/nuevo/" data-testid="nuevo-paciente">Registrar paciente</Link>}
      </div>
      <div className="card">
        <div className="fila">
          <div style={{ flex: 2, minWidth: 220 }}>
            <label htmlFor="q">Buscar</label>
            <input id="q" placeholder="DNI, historia clínica o nombre" value={q}
                   onChange={(e) => { setQ(e.target.value); setPagina(0); }} data-testid="buscar-paciente" />
          </div>
          <div style={{ flex: 1, minWidth: 160 }}>
            <label htmlFor="estado">Estado</label>
            <select id="estado" value={estado} onChange={(e) => { setEstado(e.target.value); setPagina(0); }}>
              <option value="">Todos</option>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
              <option value="FALLECIDO">Fallecido</option>
            </select>
          </div>
        </div>
      </div>
      {error && <div className="alerta bad">{error}</div>}
      {datos && (
        <>
          <div className="tabla">
            <table data-testid="tabla-pacientes">
              <thead>
                <tr><th>Historia</th><th>DNI</th><th>Paciente</th><th className="num">Edad</th><th>Estado</th></tr>
              </thead>
              <tbody>
                {datos.contenido.map((p) => (
                  <tr key={p.id}>
                    <td><Link href={`/pacientes/ficha/?id=${p.id}`}>{p.historiaClinica}</Link></td>
                    <td>{p.dni}</td>
                    <td>{p.apellidos}, {p.nombres}</td>
                    <td className="num">{edad(p.fechaNacimiento)}</td>
                    <td><EstadoChip estado={p.estado} /></td>
                  </tr>
                ))}
                {datos.contenido.length === 0 && <tr><td colSpan={5} className="vacio">Sin resultados</td></tr>}
              </tbody>
            </table>
          </div>
          <Paginador pagina={datos} onCambiar={setPagina} />
        </>
      )}
    </>
  );
}
