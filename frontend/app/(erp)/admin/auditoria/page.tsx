'use client';

import { useCallback, useEffect, useState } from 'react';
import { api, ApiError, Pagina, qs } from '@/lib/api';
import { RegistroAuditoria } from '@/lib/tipos';
import { fechaIso } from '@/lib/validaciones';
import { Paginador } from '@/components/Paginador';

export default function Auditoria() {
  const hoy = fechaIso(new Date());
  const [desde, setDesde] = useState(hoy);
  const [hasta, setHasta] = useState(hoy);
  const [entidad, setEntidad] = useState('');
  const [pagina, setPagina] = useState(0);
  const [datos, setDatos] = useState<Pagina<RegistroAuditoria> | null>(null);
  const [error, setError] = useState('');

  const cargar = useCallback(async () => {
    try {
      setDatos(await api<Pagina<RegistroAuditoria>>('/auditoria' + qs({ desde, hasta, entidad, page: pagina, size: 20 })));
      setError('');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error');
    }
  }, [desde, hasta, entidad, pagina]);

  useEffect(() => { cargar(); }, [cargar]);

  return (
    <>
      <h1>Bitácora de auditoría</h1>
      <p className="sub">Toda operación de escritura queda registrada</p>
      <div className="card fila">
        <div><label htmlFor="desde">Desde</label><input id="desde" type="date" value={desde} onChange={(e) => { setDesde(e.target.value); setPagina(0); }} /></div>
        <div><label htmlFor="hasta">Hasta</label><input id="hasta" type="date" value={hasta} onChange={(e) => { setHasta(e.target.value); setPagina(0); }} /></div>
        <div><label htmlFor="entidad">Entidad</label>
          <select id="entidad" value={entidad} onChange={(e) => { setEntidad(e.target.value); setPagina(0); }}>
            <option value="">Todas</option><option>Paciente</option><option>Producto</option><option>Usuario</option>
          </select>
        </div>
      </div>
      {error && <div className="alerta bad">{error}</div>}
      {datos && (
        <>
          <div className="tabla"><table data-testid="tabla-auditoria">
            <thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Entidad</th><th>Detalle</th></tr></thead>
            <tbody>{datos.contenido.map((r) => (
              <tr key={r.id}><td>{new Date(r.fecha).toLocaleString('es-PE')}</td><td>{r.usuario}</td><td>{r.accion}</td>
                <td>{r.entidad} {r.entidadId}</td><td>{r.detalle ?? '—'}</td></tr>))}
              {datos.contenido.length === 0 && <tr><td colSpan={5} className="vacio">Sin registros en el rango</td></tr>}
            </tbody>
          </table></div>
          <Paginador pagina={datos} onCambiar={setPagina} />
        </>
      )}
    </>
  );
}
