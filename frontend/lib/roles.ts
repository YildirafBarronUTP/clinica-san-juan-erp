export type Rol = 'ADMIN' | 'ADMISION' | 'MEDICO' | 'FARMACIA' | 'CAJA';

export interface ItemMenu {
  href: string;
  etiqueta: string;
  roles: Rol[];
  grupo: string;
}

/** Menú del ERP. Debe coincidir con los @PreAuthorize del backend (lo verifican las pruebas). */
export const MENU: ItemMenu[] = [
  { href: '/dashboard/', etiqueta: 'Inicio', roles: ['ADMIN', 'ADMISION', 'MEDICO', 'FARMACIA', 'CAJA'], grupo: 'General' },
  { href: '/pacientes/', etiqueta: 'Pacientes', roles: ['ADMIN', 'ADMISION', 'MEDICO'], grupo: 'Clínica' },
  { href: '/farmacia/productos/', etiqueta: 'Productos', roles: ['ADMIN', 'FARMACIA'], grupo: 'Farmacia' },
  { href: '/farmacia/inventario/', etiqueta: 'Inventario y alertas', roles: ['ADMIN', 'FARMACIA'], grupo: 'Farmacia' },
  { href: '/admin/usuarios/', etiqueta: 'Usuarios', roles: ['ADMIN'], grupo: 'Administración' },
  { href: '/admin/auditoria/', etiqueta: 'Auditoría', roles: ['ADMIN'], grupo: 'Administración' },
];

export const NOMBRE_ROL: Record<Rol, string> = {
  ADMIN: 'Administrador',
  ADMISION: 'Admisión',
  MEDICO: 'Médico',
  FARMACIA: 'Farmacia',
  CAJA: 'Caja',
};

export const menuPara = (rol: Rol) => MENU.filter((i) => i.roles.includes(rol));

export function puedeVer(rol: Rol, ruta: string): boolean {
  const item = MENU.filter((i) => ruta.startsWith(i.href)).sort((a, b) => b.href.length - a.href.length)[0];
  return item ? item.roles.includes(rol) : true;
}
