# Clínica San Juan ERP

ERP clínico para el proyecto final de **Pruebas de Software** (UTP, 2026-2).
Ciclo de atención ambulatoria: pacientes, citas, atención, farmacia e inventario, caja y reportes,
con seguridad por roles y auditoría.

| Parte | Tecnología | Carpeta |
| --- | --- | --- |
| Backend | Spring Boot 3.5, Java 21, Spring Security + JWT, Flyway | `backend/` |
| Base de datos | PostgreSQL 16 (Neon en staging) | `backend/src/main/resources/db/migration` |
| Frontend | Next.js 15 (export estático para Hostinger) | `frontend/` |
| E2E y accesibilidad | Playwright + axe-core | `e2e/` |
| Rendimiento | k6 | `perf/` |
| CI | GitHub Actions | `.github/workflows/ci.yml` |

## Estado por sprint

| Módulo | Estado |
| --- | --- |
| Seguridad (login JWT, 5 roles, bloqueo por intentos, usuarios) | Listo |
| Auditoría (bitácora de escrituras) | Listo |
| Pacientes (expediente, DNI, estados, alergias, búsqueda) | Listo |
| Farmacia e inventario (lotes, FEFO, kardex, costo promedio, alertas, ajustes) | Listo |
| Citas, Atención (receta), Caja, Reportes | Sprints 1 a 4 |

## Levantar todo en local

Con Docker:

```bash
docker compose up --build
```

- Frontend: http://localhost:3000
- API: http://localhost:8080, documentación en http://localhost:8080/swagger-ui.html
- Usuarios de la semilla: `admin`, `admision`, `medico`, `farmacia`, `caja`. Contraseña: `Clinica2026`

Sin Docker (PostgreSQL instalado, base `csj_erp` con usuario `csj`/`csj`):

```bash
cd backend && SEED=dev ./mvnw spring-boot:run
cd frontend && cp .env.example .env.local && npm install && npm run dev
```

## Pruebas

| Comando | Qué ejecuta |
| --- | --- |
| `cd backend && ./mvnw test` | Unitarias (JUnit 5, Mockito) + cobertura JaCoCo (falla si un paquete `service` baja de 80 %) |
| `cd backend && ./mvnw verify` | Lo anterior + integración (`*IT`) con PostgreSQL en Testcontainers. Sin Docker: `TEST_DB_URL=jdbc:postgresql://localhost:5432/csj_test ./mvnw verify` |
| `cd frontend && npm test` | Unitarias de frontend (Vitest + Testing Library) |
| `cd e2e && npm test` | E2E Playwright (escritorio y móvil) + accesibilidad WCAG 2.1 AA. Requiere `docker compose -f docker-compose.yml -f docker-compose.test.yml up -d` |
| `cd e2e && npm run smoke` | Solo los casos `@smoke` |
| `k6 run perf/carga-basica.js` | Carga: 50 usuarios virtuales sobre búsqueda de pacientes y alertas |

Reporte de cobertura: `backend/target/site/jacoco/index.html`. Reporte E2E: `cd e2e && npm run report`.

Cada prueba lleva el ID de su caso (`CP-PAC-004`, `CP-FAR-012`, `CP-E2E-020`…) en el nombre,
para armar la matriz de trazabilidad requisito, caso y prueba.

## Documentación

- [Datos de prueba y casos borde de la semilla](docs/datos-de-prueba.md)
- [Despliegue en Render, Neon y Hostinger](docs/despliegue.md)

## Equipo

Barrón Huamaní, Yildiraf Jesús · Flores Anastacio, Luis Arturo · Sotomayor Suasnabar, Frank Grheg
