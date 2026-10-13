// Cliente HTTP del ERP. El JWT se guarda en sessionStorage: se borra al cerrar la pestaña.

export const API_URL = (process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const CLAVE_SESION = 'csj.sesion';

export interface Sesion {
  token: string;
  expiraEn: string;
  username: string;
  nombreCompleto: string;
  rol: import('./roles').Rol;
}

export interface CampoError {
  campo: string;
  mensaje: string;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public codigo: string,
    mensaje: string,
    public campos: CampoError[] = [],
  ) {
    super(mensaje);
  }

  /** Errores por campo para mostrarlos bajo cada input. */
  porCampo(): Record<string, string> {
    return Object.fromEntries(this.campos.map((c) => [c.campo, c.mensaje]));
  }
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

export function leerSesion(): Sesion | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.sessionStorage.getItem(CLAVE_SESION);
    if (!raw) return null;
    const s = JSON.parse(raw) as Sesion;
    if (new Date(s.expiraEn).getTime() <= Date.now()) {
      window.sessionStorage.removeItem(CLAVE_SESION);
      return null;
    }
    return s;
  } catch {
    return null;
  }
}

export function guardarSesion(s: Sesion | null) {
  if (s) window.sessionStorage.setItem(CLAVE_SESION, JSON.stringify(s));
  else window.sessionStorage.removeItem(CLAVE_SESION);
}

export async function api<T>(ruta: string, opciones: RequestInit & { json?: unknown } = {}): Promise<T> {
  const { json, headers, ...resto } = opciones;
  const sesion = leerSesion();
  let res: Response;
  try {
    res = await fetch(`${API_URL}/api/v1${ruta}`, {
      ...resto,
      headers: {
        ...(json !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(sesion ? { Authorization: `Bearer ${sesion.token}` } : {}),
        ...headers,
      },
      body: json !== undefined ? JSON.stringify(json) : resto.body,
    });
  } catch {
    throw new ApiError(0, 'SIN_CONEXION', 'No se pudo conectar con el servidor. Verifique que el backend esté encendido.');
  }

  if (res.status === 401 && sesion && !ruta.startsWith('/auth/login')) {
    guardarSesion(null);
    if (typeof window !== 'undefined') window.location.href = '/login/?expirada=1';
  }
  if (res.status === 204) return undefined as T;

  const cuerpo = await res.json().catch(() => null);
  if (!res.ok) {
    throw new ApiError(res.status, cuerpo?.codigo ?? 'ERROR', cuerpo?.mensaje ?? `Error ${res.status}`, cuerpo?.campos ?? []);
  }
  return cuerpo as T;
}

export const qs = (params: Record<string, string | number | undefined | null>) => {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.set(k, String(v));
  });
  const s = p.toString();
  return s ? `?${s}` : '';
};
