'use client';

import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, guardarSesion, leerSesion, Sesion } from './api';

interface AuthCtx {
  sesion: Sesion | null;
  cargando: boolean;
  login: (username: string, password: string) => Promise<Sesion>;
  logout: () => void;
}

const Ctx = createContext<AuthCtx | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    setSesion(leerSesion());
    setCargando(false);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const s = await api<Sesion>('/auth/login', { method: 'POST', json: { username, password } });
    guardarSesion(s);
    setSesion(s);
    return s;
  }, []);

  const logout = useCallback(() => {
    guardarSesion(null);
    setSesion(null);
  }, []);

  return <Ctx.Provider value={{ sesion, cargando, login, logout }}>{children}</Ctx.Provider>;
}

export function useAuth() {
  const c = useContext(Ctx);
  if (!c) throw new Error('useAuth fuera de AuthProvider');
  return c;
}
