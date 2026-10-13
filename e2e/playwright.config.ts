import { defineConfig, devices } from '@playwright/test';

// Requiere el backend con perfil "test" (expone /test-support/reset) y el frontend servido.
// Con Docker: `docker compose -f docker-compose.yml -f docker-compose.test.yml up -d` desde la raíz.
export const FRONT_URL = process.env.FRONT_URL ?? 'http://localhost:3000';
export const API_URL = process.env.API_URL ?? 'http://localhost:8080';

export default defineConfig({
  testDir: './specs',
  fullyParallel: false, // todas las specs comparten la misma base y la reinician
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: FRONT_URL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'es-PE',
    timezoneId: 'America/Lima',
    launchOptions: process.env.PW_CHROMIUM_PATH ? { executablePath: process.env.PW_CHROMIUM_PATH } : {},
  },
  projects: [
    { name: 'escritorio', use: { ...devices['Desktop Chrome'] } },
    { name: 'movil', use: { ...devices['Pixel 7'] }, grep: /@movil/ },
  ],
});
