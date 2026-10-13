import { expect, test } from '@playwright/test';
import { ingresar, ingresarComo, reiniciarDatos } from './utilidades';

test.beforeEach(reiniciarDatos);

test('CP-E2E-001 Login correcto lleva al dashboard con el rol visible @smoke @regression @movil', async ({ page }) => {
  await ingresarComo(page, 'admin');
  await expect(page.getByTestId('usuario-rol')).toHaveText('Administrador');
  await expect(page.getByTestId('kpi-pacientes')).toBeVisible();
});

test('CP-E2E-002 Credenciales inválidas muestran el error del backend @smoke @regression', async ({ page }) => {
  await ingresar(page, 'admin', 'incorrecta');
  await expect(page.getByTestId('login-error')).toHaveText('Usuario o contraseña incorrectos');
  await expect(page).toHaveURL(/\/login\/$/);
});

test('CP-E2E-003 Sin sesión, una página interna redirige al login @regression', async ({ page }) => {
  await page.goto('/pacientes/');
  await expect(page).toHaveURL(/\/login\/$/);
});

test('CP-E2E-004 FARMACIA no ve Pacientes en el menú ni entra por URL @regression', async ({ page }) => {
  await ingresarComo(page, 'farmacia');
  const menu = page.getByRole('navigation', { name: 'Menú principal' });
  await expect(menu.getByRole('link', { name: 'Productos' })).toBeVisible();
  await expect(menu.getByRole('link', { name: 'Pacientes' })).toHaveCount(0);
  await page.goto('/pacientes/');
  await expect(page.getByTestId('sin-permiso')).toBeVisible();
});

test('CP-E2E-005 Cerrar sesión vuelve al login @regression', async ({ page }) => {
  await ingresarComo(page, 'medico');
  await page.getByRole('button', { name: 'Cerrar sesión' }).click();
  await expect(page).toHaveURL(/\/login\/$/);
  await page.goto('/dashboard/');
  await expect(page).toHaveURL(/\/login\/$/);
});
