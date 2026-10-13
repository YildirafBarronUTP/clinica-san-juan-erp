import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';

const replace = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace, push: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
}));

import LoginPage from '@/app/login/page';
import { AuthProvider } from '@/lib/auth';

function renderLogin() {
  return render(<AuthProvider><LoginPage /></AuthProvider>);
}

describe('CP-FE-020 Pantalla de login', () => {
  beforeEach(() => {
    replace.mockReset();
    sessionStorage.clear();
  });

  it('muestra el mensaje del backend cuando las credenciales son inválidas', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ status: 401, codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'Usuario o contraseña incorrectos', campos: [] }),
      { status: 401 })));
    renderLogin();
    await userEvent.type(screen.getByLabelText('Usuario'), 'admin');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'mala');
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));
    expect(await screen.findByTestId('login-error')).toHaveTextContent('Usuario o contraseña incorrectos');
    expect(replace).not.toHaveBeenCalled();
  });

  it('guarda la sesión y redirige al dashboard cuando el login es correcto', async () => {
    const expira = new Date(Date.now() + 3600_000).toISOString();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ token: 't', expiraEn: expira, username: 'admin', nombreCompleto: 'Admin', rol: 'ADMIN' }), { status: 200 })));
    renderLogin();
    await userEvent.type(screen.getByLabelText('Usuario'), 'admin');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Clinica2026');
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));
    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/dashboard/'));
    expect(JSON.parse(sessionStorage.getItem('csj.sesion')!).rol).toBe('ADMIN');
  });

  it('informa cuando el backend no responde', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    renderLogin();
    await userEvent.type(screen.getByLabelText('Usuario'), 'admin');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'x');
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));
    expect(await screen.findByTestId('login-error')).toHaveTextContent('No se pudo conectar');
  });
});
