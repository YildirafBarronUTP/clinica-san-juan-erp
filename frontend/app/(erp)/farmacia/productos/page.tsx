'use client';

import Link from 'next/link';
import { FormEvent, useCallback, useEffect, useState } from 'react';
import { api, ApiError, Pagina, qs } from '@/lib/api';
import { Producto } from '@/lib/tipos';
import { soles } from '@/lib/validaciones';
import { Paginador } from '@/components/Paginador';
import { Campo } from '@/components/Campo';

const VACIO = { codigo: '', nombre: '', principioActivo: '', presentacion: '', precioVenta: '', stockMinimo: '' };

export default function Productos() {
  const [q, setQ] = useState('');
  const [pagina, setPagina] = useState(0);
  const [datos, setDatos] = useState<Pagina<Producto> | null>(null);
  const [nuevo, setNuevo] = useState(false);
  const [f, setF] = useState(VACIO);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [error, setError] = useState('');

  const cargar = useCallback(async () => {
    try {
      setDatos(await api<Pagina<Producto>>('/productos' + qs({ q, page: pagina, size: 15 })));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error');
    }
  }, [q, pagina]);

  useEffect(() => {
    const t = setTimeout(cargar, 250);
    return () => clearTimeout(t);
  }, [cargar]);

  async function crear(e: FormEvent) {
    e.preventDefault();
    try {
      await api('/productos', { method: 'POST', json: { ...f, precioVenta: Number(f.precioVenta), stockMinimo: Number(f.stockMinimo) } });
      setF(VACIO); setNuevo(false); setErrores({}); setError('');
      cargar();
    } catch (err) {
      if (err instanceof ApiError) { setErrores(err.porCampo()); setError(err.message); }
    }
  }

  const set = (k: keyof typeof VACIO) => (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value });

  return (
    <>
      <div className="fila entre">
        <div><h1>Productos</h1><p className="sub">Catálogo de farmacia con stock por lote</p></div>
        <button onClick={() => setNuevo(!nuevo)} data-testid="nuevo-producto">{nuevo ? 'Cancelar' : 'Nuevo producto'}</button>
      </div>
      {error && <div className="alerta bad" role="alert">{error}</div>}
      {nuevo && (
        <form className="card" onSubmit={crear} noValidate>
          <div className="grid">
            <Campo etiqueta="Código" nombre="codigo" value={f.codigo} onChange={set('codigo')} error={errores.codigo} />
            <Campo etiqueta="Nombre" nombre="nombre" value={f.nombre} onChange={set('nombre')} error={errores.nombre} />
            <Campo etiqueta="Principio activo" nombre="principioActivo" value={f.principioActivo} onChange={set('principioActivo')} error={errores.principioActivo} />
            <Campo etiqueta="Presentación" nombre="presentacion" value={f.presentacion} onChange={set('presentacion')} error={errores.presentacion} />
            <Campo etiqueta="Precio de venta (S/)" nombre="precioVenta" type="number" step="0.01" value={f.precioVenta} onChange={set('precioVenta')} error={errores.precioVenta} />
            <Campo etiqueta="Stock mínimo" nombre="stockMinimo" type="number" value={f.stockMinimo} onChange={set('stockMinimo')} error={errores.stockMinimo} />
          </div>
          <button type="submit">Guardar producto</button>
        </form>
      )}
      <div className="card">
        <label htmlFor="q">Buscar</label>
        <input id="q" placeholder="Nombre, principio activo o código" value={q} onChange={(e) => { setQ(e.target.value); setPagina(0); }} />
      </div>
      {datos && (
        <>
          <div className="tabla">
            <table data-testid="tabla-productos">
              <thead><tr><th>Código</th><th>Producto</th><th>Presentación</th><th className="num">Precio</th>
                <th className="num">Disponible</th><th className="num">Mínimo</th><th>Estado</th></tr></thead>
              <tbody>
                {datos.contenido.map((p) => (
                  <tr key={p.id}>
                    <td><Link href={`/farmacia/producto/?id=${p.id}`}>{p.codigo}</Link></td>
                    <td>{p.nombre}</td>
                    <td>{p.presentacion}</td>
                    <td className="num">{soles(p.precioVenta)}</td>
                    <td className="num">{p.stockDisponible}</td>
                    <td className="num">{p.stockMinimo}</td>
                    <td>{!p.activo ? <span className="chip neutro">Inactivo</span>
                      : p.stockDisponible <= p.stockMinimo ? <span className="chip warn">Reponer</span>
                      : <span className="chip ok">OK</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Paginador pagina={datos} onCambiar={setPagina} />
        </>
      )}
    </>
  );
}
