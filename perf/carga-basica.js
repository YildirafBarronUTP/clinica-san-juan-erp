// Prueba de carga con k6 (https://k6.io).  Uso:
//   k6 run perf/carga-basica.js                       -> carga (50 usuarios virtuales)
//   k6 run -e PERFIL=smoke perf/carga-basica.js       -> verificación rápida en CI
// Umbrales iniciales del plan: p95 < 500 ms y menos de 1 % de errores. Ajustar con la línea base.
import http from 'k6/http';
import { check, group, sleep } from 'k6';

const API = (__ENV.API_URL || 'http://localhost:8080') + '/api/v1';
const PASSWORD = __ENV.SEED_PASSWORD || 'Clinica2026';
const smoke = __ENV.PERFIL === 'smoke';

export const options = {
  scenarios: {
    busqueda: smoke
      ? { executor: 'constant-vus', vus: 2, duration: '15s' }
      : {
          executor: 'ramping-vus',
          stages: [
            { duration: '30s', target: 20 },
            { duration: '1m', target: 50 },
            { duration: '30s', target: 0 },
          ],
        },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:buscar_pacientes}': ['p(95)<500'],
    'http_req_duration{endpoint:alertas}': ['p(95)<500'],
  },
};

function login(usuario) {
  const r = http.post(`${API}/auth/login`, JSON.stringify({ username: usuario, password: PASSWORD }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(r, { 'login 200': (x) => x.status === 200 });
  return { headers: { Authorization: `Bearer ${r.json('token')}` } };
}

export function setup() {
  return { admision: login('admision'), farmacia: login('farmacia') };
}

const TERMINOS = ['quispe', 'flores', '7000012', 'ana', 'HC-000150', 'garcía', 'mamani'];

export default function (t) {
  group('Admisión busca pacientes', () => {
    const q = TERMINOS[Math.floor(Math.random() * TERMINOS.length)];
    const r = http.get(`${API}/pacientes?q=${encodeURIComponent(q)}&size=20`, { ...t.admision, tags: { endpoint: 'buscar_pacientes' } });
    check(r, { 'buscar 200': (x) => x.status === 200 });
  });
  group('Farmacia consulta alertas', () => {
    const r = http.get(`${API}/inventario/alertas`, { ...t.farmacia, tags: { endpoint: 'alertas' } });
    check(r, { 'alertas 200': (x) => x.status === 200 });
  });
  sleep(1);
}
