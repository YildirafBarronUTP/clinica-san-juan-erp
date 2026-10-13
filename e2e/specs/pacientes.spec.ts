import { expect, test } from '@playwright/test';
import { ingresarComo, reiniciarDatos } from './utilidades';

test.beforeEach(async ({ page }) => {
  await reiniciarDatos();
  await ingresarComo(page, 'admision');
});

test('CP-E2E-010 Registrar un paciente y ver su ficha @smoke @regression', async ({ page }) => {
  await page.getByRole('link', { name: 'Pacientes' }).click();
  await page.getByTestId('nuevo-paciente').click();
  await page.getByLabel('DNI').fill('45678912');
  await page.getByLabel('Nombres').fill('Ana María');
  await page.getByLabel('Apellidos').fill('Pérez Quispe');
  await page.getByLabel('Fecha de nacimiento').fill('1990-05-01');
  await page.getByLabel('Sexo').selectOption('F');
  await page.getByLabel('Teléfono (opcional)').fill('987654321');
  await page.getByTestId('guardar-paciente').click();

  await expect(page.getByTestId('ficha-mensaje')).toHaveText('Paciente registrado correctamente');
  await expect(page.getByTestId('ficha-nombre')).toHaveText('Pérez Quispe, Ana María');
  await expect(page.getByText('HC-000301')).toBeVisible();
});

test('CP-E2E-011 Validación en pantalla: DNI corto y teléfono inválido no llegan al backend @regression', async ({ page }) => {
  await page.goto('/pacientes/nuevo/');
  await page.getByLabel('DNI').fill('1234567');
  await page.getByLabel('Teléfono (opcional)').fill('12345');
  await page.getByTestId('guardar-paciente').click();
  await expect(page.getByTestId('error-dni')).toHaveText('El DNI debe tener exactamente 8 dígitos');
  await expect(page.getByTestId('error-telefono')).toBeVisible();
  await expect(page.getByTestId('error-nombres')).toBeVisible();
  await expect(page).toHaveURL(/\/pacientes\/nuevo\/$/);
});

test('CP-E2E-012 DNI duplicado muestra el error del backend en el campo @regression', async ({ page }) => {
  await page.goto('/pacientes/nuevo/');
  await page.getByLabel('DNI').fill('70000001');
  await page.getByLabel('Nombres').fill('Duplicado');
  await page.getByLabel('Apellidos').fill('Prueba');
  await page.getByLabel('Fecha de nacimiento').fill('1980-01-01');
  await page.getByLabel('Sexo').selectOption('M');
  await page.getByTestId('guardar-paciente').click();
  await expect(page.getByTestId('form-error')).toHaveText('Ya existe un paciente con ese DNI');
  await expect(page.getByTestId('error-dni')).toBeVisible();
});

test('CP-E2E-013 Buscar por DNI y ver las alergias del paciente semilla @regression @movil', async ({ page }) => {
  await page.goto('/pacientes/');
  await page.getByTestId('buscar-paciente').fill('70000001');
  const tabla = page.getByTestId('tabla-pacientes');
  await expect(tabla.locator('tbody tr')).toHaveCount(1);
  await tabla.getByRole('link', { name: 'HC-000001' }).click();
  await expect(page.getByTestId('lista-alergias')).toContainText('penicilina');
});

test('CP-E2E-014 Marcar como fallecido deja el expediente en solo lectura @regression', async ({ page }) => {
  await page.goto('/pacientes/ficha/?id=3');
  page.once('dialog', (d) => d.accept());
  await page.getByTestId('estado-FALLECIDO').click();
  await expect(page.getByTestId('ficha-mensaje')).toHaveText('Estado cambiado a FALLECIDO');
  await expect(page.getByRole('button', { name: 'Editar datos' })).toHaveCount(0);
});
