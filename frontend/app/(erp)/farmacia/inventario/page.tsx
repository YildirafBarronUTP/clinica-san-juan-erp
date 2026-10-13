'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { api, ApiError } from '@/lib/api';
import { AlertaLote, Alertas, Valorizacion } from '@/lib/tipos';
import { soles } from '@/lib/validaciones';

function TablaLotes({ lotes, testid }: { lotes: AlertaLote[]; testid: string }) {
  if (lotes.length === 0) return <p className="sub">Ninguno</p>;
  return (
    <div className="tabla"><table data-testid={testid}>
      <thead><tr><th>Producto</th><th>Lote</th><th>Vence</th><th className="num">Días</th><th className="num">Cantidad</th></tr></thead>
      <tbody>{lotes.map((l) => (
        <tr key={l.loteId}>
          <td><Link href={`/farmacia/producto/?id=${l.productoId}`}>{l.producto}</Link></td>
          <td>{l.numeroLote}</td><td>{l.fechaVencimiento}</td>
          <td className="num">{l.diasRestantes}</td><td className="num">{l.cantidad}</td>
        </tr>))}
      </tbody>
    </table></div>
  );
}

export default function Inventario() {
  const [a, setA] = useState<Alertas | null>(null);
  const [v, setV] = useState<Valorizacion | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([api<Alertas>('/inventario/alertas'), api<Valorizacion>('/inventario/valorizacion')])
      .then(([al, va]) => { setA(al); setV(va); })
      .catch((e) => setError(e instanceof ApiError ? e.message : 'Error'));
  }, []);

  if (error) return <div className="alerta bad">{error}</div>;
  if (!a || !v) return <p>Cargando…</p>;

  return (
    <>
      <h1>Inventario y alertas</h1>
      <p className="sub">Corte al {a.fecha}</p>

      <div className="card">
        <h2>Stock en el mínimo o por debajo ({a.bajoMinimo.length})</h2>
        {a.bajoMinimo.length === 0 ? <p className="sub">Ninguno</p> : (
          <div className="tabla"><table data-testid="tabla-bajo-minimo">
            <thead><tr><th>Código</th><th>Producto</th><th className="num">Disponible</th><th className="num">Mínimo</th></tr></thead>
            <tbody>{a.bajoMinimo.map((s) => (
              <tr key={s.productoId}>
                <td><Link href={`/farmacia/producto/?id=${s.productoId}`}>{s.codigo}</Link></td><td>{s.nombre}</td>
                <td className="num">{s.stockDisponible}</td><td className="num">{s.stockMinimo}</td>
              </tr>))}
            </tbody>
          </table></div>
        )}
      </div>

      <div className="card"><h2>Lotes por vencer en 30 días ({a.porVencer.length})</h2><TablaLotes lotes={a.porVencer} testid="tabla-por-vencer" /></div>
      <div className="card"><h2>Lotes vencidos con saldo ({a.vencidos.length})</h2><TablaLotes lotes={a.vencidos} testid="tabla-vencidos" /></div>

      <div className="card">
        <div className="fila entre"><h2>Valorización a costo promedio</h2><strong data-testid="valorizacion-total">{soles(v.total)}</strong></div>
        <div className="tabla"><table>
          <thead><tr><th>Código</th><th>Producto</th><th className="num">Stock</th><th className="num">Costo prom.</th><th className="num">Valor</th></tr></thead>
          <tbody>{v.lineas.map((l) => (
            <tr key={l.productoId}><td>{l.codigo}</td><td>{l.nombre}</td><td className="num">{l.stock}</td>
              <td className="num">{Number(l.costoPromedio).toFixed(4)}</td><td className="num">{soles(l.valor)}</td></tr>))}
          </tbody>
        </table></div>
      </div>
    </>
  );
}
