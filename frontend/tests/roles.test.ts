import { menuPara, puedeVer } from '@/lib/roles';

describe('CP-FE-010 Menú por rol', () => {
  it('ADMIN ve todos los módulos', () => {
    expect(menuPara('ADMIN').map((i) => i.href)).toContain('/admin/usuarios/');
  });
  it('FARMACIA no ve pacientes ni administración', () => {
    const rutas = menuPara('FARMACIA').map((i) => i.href);
    expect(rutas).toContain('/farmacia/inventario/');
    expect(rutas).not.toContain('/pacientes/');
    expect(rutas).not.toContain('/admin/usuarios/');
  });
  it('MEDICO ve pacientes pero no inventario', () => {
    expect(puedeVer('MEDICO', '/pacientes/ficha/')).toBe(true);
    expect(puedeVer('MEDICO', '/farmacia/inventario/')).toBe(false);
  });
  it('CAJA no entra por URL directa a usuarios', () => {
    expect(puedeVer('CAJA', '/admin/usuarios/')).toBe(false);
  });
});
