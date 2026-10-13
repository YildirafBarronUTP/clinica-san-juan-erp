'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { useAuth } from '@/lib/auth';
import { menuPara, NOMBRE_ROL, puedeVer } from '@/lib/roles';

export default function Shell({ children }: { children: React.ReactNode }) {
  const { sesion, cargando, logout } = useAuth();
  const router = useRouter();
  const ruta = usePathname();

  useEffect(() => {
    if (!cargando && !sesion) router.replace('/login/');
  }, [cargando, sesion, router]);

  if (cargando || !sesion) return null;

  const items = menuPara(sesion.rol);
  const grupos = [...new Set(items.map((i) => i.grupo))];
  const permitido = puedeVer(sesion.rol, ruta);

  return (
    <div className="shell">
      <nav className="side" aria-label="Menú principal">
        <div className="marca">Clínica San Juan<small>ERP clínico</small></div>
        {grupos.map((g) => (
          <div key={g}>
            <div className="grupo">{g}</div>
            {items.filter((i) => i.grupo === g).map((i) => (
              <Link key={i.href} href={i.href} className={ruta.startsWith(i.href) ? 'activo' : ''}>{i.etiqueta}</Link>
            ))}
          </div>
        ))}
        <div className="usuario">
          <strong data-testid="usuario-nombre">{sesion.nombreCompleto}</strong>
          <span data-testid="usuario-rol">{NOMBRE_ROL[sesion.rol]}</span>
          <div style={{ marginTop: 8 }}>
            <button className="sec" onClick={() => { logout(); router.replace('/login/'); }}>Cerrar sesión</button>
          </div>
        </div>
      </nav>
      <main className="main">
        {permitido ? children : (
          <div className="alerta bad" data-testid="sin-permiso">Su rol no tiene acceso a esta sección.</div>
        )}
      </main>
    </div>
  );
}
