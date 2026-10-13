'use client';

import { FormEvent, Suspense, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { ApiError } from '@/lib/api';

function FormLogin() {
  const { login } = useAuth();
  const router = useRouter();
  const expirada = useSearchParams().get('expirada');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setError('');
    setEnviando(true);
    try {
      await login(username.trim(), password);
      router.replace('/dashboard/');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Error inesperado');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form className="card" onSubmit={enviar} aria-label="Iniciar sesión">
      <h1>Clínica San Juan</h1>
      <p className="sub">Ingrese con su usuario del ERP</p>
      {expirada && !error && <div className="alerta warn">Su sesión expiró. Ingrese nuevamente.</div>}
      {error && <div className="alerta bad" role="alert" data-testid="login-error">{error}</div>}
      <div className="campo">
        <label htmlFor="username">Usuario</label>
        <input id="username" autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} required />
      </div>
      <div className="campo">
        <label htmlFor="password">Contraseña</label>
        <input id="password" type="password" autoComplete="current-password" value={password}
               onChange={(e) => setPassword(e.target.value)} required />
      </div>
      <button type="submit" disabled={enviando} style={{ width: '100%', justifyContent: 'center' }}>
        {enviando ? 'Ingresando…' : 'Ingresar'}
      </button>
    </form>
  );
}

export default function LoginPage() {
  return (
    <main className="login">
      <Suspense>
        <FormLogin />
      </Suspense>
    </main>
  );
}
