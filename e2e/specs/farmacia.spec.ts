import { expect, test } from '@playwright/test';
import { ingresarComo, reiniciarDatos } from './utilidades';

function enDias(n: number) {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

test.beforeEach(async ({ page }) => {
  await reiniciarDatos();
  await ingresarComo(page, 'farmacia');
});

test('CP-E2E-020 Salida por FEFO usa el lote que vence hoy y salta el vencido @smoke @regression', async ({ page }) => {
  await page.goto('/farmacia/producto/?id=1'); // MED-001: lote vencido, que vence hoy, mañana y a 400 días
  await expect(page.getByTestId('stock-disponible')).toHaveText('600');
  await page.getByLabel('Cantidad', { exact: true }).nth(1).fill('50');
  await page.getByLabel('Referencia').fill('Receta E2E');
  await page.getByTestId('registrar-salida').click();
  await expect(page.getByTestId('producto-mensaje')).toHaveText('Salida registrada: 40 de L001-002, 10 de L001-003');
  await expect(page.getByTestId('stock-disponible')).toHaveText('550');
  await expect(page.getByTestId('tabla-kardex').locator('tbody tr').last()).toContainText('580');
});

test('CP-E2E-021 Pedir más de lo disponible muestra stock insuficiente @regression', async ({ page }) => {
  await page.goto('/farmacia/producto/?id=15'); // MED-015 tiene 5 unidades
  await page.getByLabel('Cantidad', { exact: true }).nth(1).fill('6');
  await page.getByLabel('Referencia').fill('Excede');
  await page.getByTestId('registrar-salida').click();
  await expect(page.getByTestId('producto-error')).toContainText('Stock disponible insuficiente');
  await expect(page.getByTestId('stock-disponible')).toHaveText('5');
});

test('CP-E2E-022 Ingresar un lote actualiza stock, lotes y kardex @regression', async ({ page }) => {
  await page.goto('/farmacia/producto/?id=26'); // MED-026 sin stock
  await page.getByLabel('Número de lote').fill('e2e-01');
  await page.getByLabel('Vencimiento').fill(enDias(180));
  await page.getByLabel('Cantidad', { exact: true }).first().fill('40');
  await page.getByLabel('Costo unitario (S/)').fill('20.5');
  await page.getByTestId('ingresar-lote').click();
  await expect(page.getByTestId('producto-mensaje')).toHaveText('Lote ingresado');
  await expect(page.getByTestId('stock-disponible')).toHaveText('40');
  await expect(page.getByTestId('tabla-lotes')).toContainText('E2E-01');
});

test('CP-E2E-023 Las alertas muestran los casos borde de la semilla @smoke @regression', async ({ page }) => {
  await page.goto('/farmacia/inventario/');
  await expect(page.getByTestId('tabla-bajo-minimo')).toContainText('MED-009'); // stock igual al mínimo
  await expect(page.getByTestId('tabla-bajo-minimo')).toContainText('MED-026'); // sin stock
  await expect(page.getByTestId('tabla-vencidos')).toContainText('L001-001');
  await expect(page.getByTestId('tabla-por-vencer')).toContainText('L035-001');
  await expect(page.getByTestId('valorizacion-total')).toHaveText(/^S\/ [\d,]+\.\d{2}$/);
});
