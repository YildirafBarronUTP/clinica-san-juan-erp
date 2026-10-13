import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { ingresarComo, reiniciarDatos } from './utilidades';

// Evaluación automática WCAG 2.1 AA. Se reportan violaciones críticas y serias.
const PANTALLAS = ['/dashboard/', '/pacientes/', '/pacientes/nuevo/', '/pacientes/ficha/?id=1',
  '/farmacia/productos/', '/farmacia/producto/?id=1', '/farmacia/inventario/', '/admin/usuarios/', '/admin/auditoria/'];

test.beforeAll(reiniciarDatos);

test('CP-A11Y-001 Login sin violaciones críticas @a11y', async ({ page }) => {
  await page.goto('/login/');
  const r = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
  expect(r.violations.filter((v) => ['critical', 'serious'].includes(v.impact ?? ''))).toEqual([]);
});

for (const ruta of PANTALLAS) {
  test(`CP-A11Y-002 ${ruta} sin violaciones críticas @a11y`, async ({ page }) => {
    await ingresarComo(page, 'admin');
    await page.goto(ruta);
    await page.waitForLoadState('networkidle');
    const r = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
    const graves = r.violations.filter((v) => ['critical', 'serious'].includes(v.impact ?? ''));
    expect(graves.map((v) => `${v.id}: ${v.help}`)).toEqual([]);
  });
}
