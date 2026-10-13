'use client';

import Link from 'next/link';
import { FormEvent, Suspense, useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { api, ApiError } from '@/lib/api';
import { Lote, Movimiento, Producto } from '@/lib/tipos';
import { fechaIso, soles } from '@/lib/validaciones';
import { Campo } from '@/components/Campo';

function DetalleProducto() {
  const id = useSearchParams().get('id');
  const [p, setP] = useState<Producto | null>(null);
  const [lotes, setLotes] = useState<Lote[]>([]);
  const [kardex, setKardex] = useState<Movimiento[]>([]);
  const [mensaje, setMensaje] = useState('');
  const [error, setError] = useState('');
  const [errLote, setErrLote] = useState<Record<string, string>>({});
  const [lote, setLote] = useState({ numeroLote: '', fechaVencimiento: '', cantidad: '', costoUnitario: '' });
  const [salida, setSalida] = useState({ cantidad: '', referencia: '' });
  const hoy = fechaIso(new Date());

  const cargar = useCallback(async () => {
    const [prod, ls, k] = await Promise.all([
      api<Producto>(`/productos/${id}`), api<Lote[]>(`/productos/${id}/lotes`), api<Movimiento[]>(`/productos/${id}/kardex`),
    ]);
    setP(prod); setLotes(ls); setKardex(k);
  }, [id]);

  useEffect(() => { cargar().catch((e) => setError(e.message)); }, [cargar]);

  async function ingresar(e: FormEvent) {
    e.preventDefault();
    try {
      await api(`/productos/${id}/lotes`, { method: 'POST', json: {
        ...lote, cantidad: Number(lote.cantidad), costoUnitario: Number(lote.costoUnitario) } });
      setLote({ numeroLote: '', fechaVencimiento: '', cantidad: '', costoUnitario: '' });
      setErrLote({}); setError(''); setMensaje('Lote ingresado');
      await cargar();
    } catch (err) {
      if (err instanceof ApiError) { setErrLote(err.porCampo()); setError(err.message); setMensaje(''); }
    }
  }

  async function dispensar(e: FormEvent) {
    e.preventDefault();
    try {
      const det = await api<{ numeroLote: string; cantidad: number }[]>(`/productos/${id}/salidas`, {
        method: 'POST', json: { cantidad: Number(salida.cantidad), referencia: salida.referencia } });
      setMensaje('Salida registrada: ' + det.map((d) => `${d.cantidad} de ${d.numeroLote}`).join(', '));
      setSalida({ cantidad: '', referencia: '' }); setError('');
      await cargar();
    } catch (err) {
      if (err instanceof ApiError) { setError(err.message); setMensaje(''); }
    }
  }

  if (!p) return error ? <div className="alerta bad">{error}</div> : <p>Cargando…</p>;

  return (
    <>
      <Link href="/farmacia/productos/">← Volver a productos</Link>
      <h1 style={{ marginTop: 10 }}>{p.nombre}</h1>
      <p className="sub">{p.codigo} · {p.presentacion} · principio activo: {p.principioActivo}</p>
      <div className="grid" style={{ marginBottom: 20 }}>
        <div className="kpi"><div className="etq">Stock disponible</div><div className="valor" data-testid="stock-disponible">{p.stockDisponible}</div></div>
        <div className="kpi"><div className="etq">Stock físico (incluye vencidos)</div><div className="valor">{p.stockFisico}</div></div>
        <div className="kpi"><div className="etq">Costo promedio</div><div className="valor">{soles(p.costoPromedio)}</div></div>
        <div className="kpi"><div className="etq">Precio de venta</div><div className="valor">{soles(p.precioVenta)}</div></div>
      </div>
      {mensaje && <div className="alerta ok" data-testid="producto-mensaje">{mensaje}</div>}
      {error && <div className="alerta bad" role="alert" data-testid="producto-error">{error}</div>}

      <div className="grid">
        <form className="card" onSubmit={ingresar} noValidate>
          <h2>Ingresar lote</h2>
          <Campo etiqueta="Número de lote" nombre="numeroLote" value={lote.numeroLote} onChange={(e) => setLote({ ...lote, numeroLote: e.target.value })} error={errLote.numeroLote} />
          <Campo etiqueta="Vencimiento" nombre="fechaVencimiento" type="date" min={hoy} value={lote.fechaVencimiento} onChange={(e) => setLote({ ...lote, fechaVencimiento: e.target.value })} error={errLote.fechaVencimiento} />
          <Campo etiqueta="Cantidad" nombre="cantidad" type="number" value={lote.cantidad} onChange={(e) => setLote({ ...lote, cantidad: e.target.value })} error={errLote.cantidad} />
          <Campo etiqueta="Costo unitario (S/)" nombre="costoUnitario" type="number" step="0.0001" value={lote.costoUnitario} onChange={(e) => setLote({ ...lote, costoUnitario: e.target.value })} error={errLote.costoUnitario} />
          <button type="submit" data-testid="ingresar-lote">Ingresar</button>
        </form>
        <form className="card" onSubmit={dispensar} noValidate>
          <h2>Salida por FEFO</h2>
          <p className="sub">Se descuenta primero del lote que vence antes. Los lotes vencidos no se usan.</p>
          <Campo etiqueta="Cantidad" nombre="cantidadSalida" type="number" value={salida.cantidad} onChange={(e) => setSalida({ ...salida, cantidad: e.target.value })} />
          <Campo etiqueta="Referencia" nombre="referencia" placeholder="Ej. Receta 120" value={salida.referencia} onChange={(e) => setSalida({ ...salida, referencia: e.target.value })} />
          <button type="submit" data-testid="registrar-salida">Registrar salida</button>
        </form>
      </div>

      <div className="card">
        <h2>Lotes</h2>
        <div className="tabla"><table data-testid="tabla-lotes">
          <thead><tr><th>Lote</th><th>Vencimiento</th><th className="num">Inicial</th><th className="num">Actual</th><th className="num">Costo</th><th>Estado</th></tr></thead>
          <tbody>{lotes.map((l) => (
            <tr key={l.id}>
              <td>{l.numeroLote}</td><td>{l.fechaVencimiento}</td><td className="num">{l.cantidadInicial}</td>
              <td className="num">{l.cantidadActual}</td><td className="num">{soles(l.costoUnitario)}</td>
              <td>{l.fechaVencimiento < hoy ? <span className="chip bad">Vencido</span>
                : l.cantidadActual === 0 ? <span className="chip neutro">Agotado</span> : <span className="chip ok">Vigente</span>}</td>
            </tr>))}
            {lotes.length === 0 && <tr><td colSpan={6} className="vacio">Sin lotes</td></tr>}
          </tbody>
        </table></div>
      </div>

      <div className="card">
        <h2>Kardex</h2>
        <div className="tabla"><table data-testid="tabla-kardex">
          <thead><tr><th>Fecha</th><th>Tipo</th><th>Lote</th><th className="num">Cantidad</th><th className="num">Saldo</th><th>Referencia</th><th>Usuario</th></tr></thead>
          <tbody>{kardex.map((m) => (
            <tr key={m.id}>
              <td>{new Date(m.fecha).toLocaleString('es-PE')}</td>
              <td><span className={`chip ${m.tipo === 'ENTRADA' ? 'ok' : m.tipo === 'SALIDA' ? 'neutro' : 'warn'}`}>{m.tipo}</span></td>
              <td>{m.numeroLote ?? '—'}</td>
              <td className="num">{m.tipo === 'ENTRADA' ? '+' : '−'}{m.cantidad}</td>
              <td className="num">{m.saldo}</td><td>{m.referencia}</td><td>{m.usuario}</td>
            </tr>))}
          </tbody>
        </table></div>
      </div>
    </>
  );
}

export default function ProductoPage() {
  return <Suspense><DetalleProducto /></Suspense>;
}
