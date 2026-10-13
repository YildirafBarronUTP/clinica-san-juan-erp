import { expect, Page, request } from '@playwright/test';
import { API_URL } from '../playwright.config';

export const PASSWORD = process.env.SEED_PASSWORD ?? 'Clinica2026';

/** Restaura la base a la semilla dev antes de cada prueba. */
export async function reiniciarDatos() {
  const ctx = await request.newContext();
  const r = await ctx.post(`${API_URL}/api/v1/test-support/reset`);
  expect(r.status(), 'El backend debe correr con el perfil test').toBe(204);
  await ctx.dispose();
}

export async function ingresar(page: Page, usuario: string, password = PASSWORD) {
  await page.goto('/login/');
  await page.getByLabel('Usuario').fill(usuario);
  await page.getByLabel('Contraseña').fill(password);
  await page.getByRole('button', { name: 'Ingresar' }).click();
}

export async function ingresarComo(page: Page, usuario: string) {
  await ingresar(page, usuario);
  await expect(page).toHaveURL(/\/dashboard\/$/);
}
