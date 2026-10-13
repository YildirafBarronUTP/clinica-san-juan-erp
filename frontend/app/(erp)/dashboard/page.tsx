'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { api, Pagina } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Alertas, PacienteResumen, Valorizacion } from '@/lib/tipos';
import { soles } from '@/lib/validaciones';

export default function Dashboard() {
  const { sesion } = useAuth();
  const rol = sesion!.rol;
  const [pacientes, setPacientes] = useState<number | null>(null);
  const [alertas, setAlertas] = useState<Alertas | null>(null);
  const [valor, setValor] = useState<Valorizacion | null>(null);

  useEffect(() => {
    if (['ADMIN', 'ADMISION', 'MEDICO', 'CAJA'].includes(rol)) {
      api<Pagina<PacienteResumen>>('/pacientes?size=1&estado=ACTIVO').then((p) => setPacientes(p.totalElementos)).catch(() => {});
    }
    if (['ADMIN', 'FARMACIA'].includes(rol)) {
      api<Alertas>('/inventario/alertas').then(setAlertas).catch(() => {});
      api<Valorizacion>('/inventario/valorizacion').then(setValor).catch(() => {});
    }
  }, [rol]);

  return (
    <>
      <h1>Hola, {sesion!.nombreCompleto.split(' ')[0]}</h1>
      <p className="sub">Resumen del día</p>
      <div className="grid">
        {pacientes !== null && (
          <div className="kpi" data-testid="kpi-pacientes"><div className="etq">Pacientes activos</div><div className="valor">{pacientes}</div></div>
        )}
        {alertas && (
          <>
            <div className="kpi" data-testid="kpi-bajo-minimo"><div className="etq">Productos en stock mínimo</div><div className="valor">{alertas.bajoMinimo.length}</div></div>
            <div className="kpi" data-testid="kpi-por-vencer"><div className="etq">Lotes por vencer (30 días)</div><div className="valor">{alertas.porVencer.length}</div></div>
            <div className="kpi"><div className="etq">Lotes vencidos con saldo</div><div className="valor">{alertas.vencidos.length}</div></div>
          </>
        )}
        {valor && (
          <div className="kpi" data-testid="kpi-valorizacion"><div className="etq">Inventario valorizado</div><div className="valor">{soles(valor.total)}</div></div>
        )}
      </div>
      {alertas && (alertas.bajoMinimo.length > 0 || alertas.vencidos.length > 0) && (
        <div className="alerta warn" style={{ marginTop: 20 }}>
          Hay productos que requieren atención. <Link href="/farmacia/inventario/">Ver alertas de inventario</Link>
        </div>
      )}
    </>
  );
}
