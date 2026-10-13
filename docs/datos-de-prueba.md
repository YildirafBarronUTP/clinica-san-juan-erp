# Datos de prueba

Todos los datos son **sintéticos** (Ley 29733: ningún dato real de pacientes entra al repositorio ni al staging).
La semilla usa una semilla aleatoria fija (`app.seed.semilla`), así que siempre genera los mismos registros.

## Perfiles

| Variable `SEED` | Contenido | Uso |
| --- | --- | --- |
| `none` (por defecto) | Nada | Producción o base ya cargada |
| `dev` / `demo` | 5 usuarios, 300 pacientes, 40 productos con lotes y casos borde | Desarrollo, E2E, usabilidad, staging |
| `perf` | 10 000 pacientes y 12 lotes por producto | Pruebas de carga |

La semilla solo se carga si la tabla `usuario` está vacía. Con el perfil Spring `test`, `POST /api/v1/test-support/reset`
vacía la base y vuelve a cargar `dev` (lo usan las pruebas de integración y E2E antes de cada caso).

## Usuarios

| Usuario | Rol | Contraseña |
| --- | --- | --- |
| admin | ADMIN | `SEED_PASSWORD` (por defecto `Clinica2026`) |
| admision | ADMISION | igual |
| medico | MEDICO | igual |
| farmacia | FARMACIA | igual |
| caja | CAJA | igual |

En staging se define `SEED_PASSWORD` con otro valor.

## Pacientes

- DNI `70000001` a `70000300`, historias `HC-000001` a `HC-000300`. El siguiente registrado recibe `HC-000301`.
- Cada paciente número 25, 50, 75… está **INACTIVO** (12 en total).
- `70000001` (HC-000001) tiene **alergia a la penicilina**: caso de la receta bloqueada en el Sprint 2.

## Casos borde de farmacia (relativos a la fecha en que se carga la semilla)

| Producto | Situación | Prueba que lo usa |
| --- | --- | --- |
| MED-001 Paracetamol | Lote `L001-001` vencido hace 10 días (30 u.), `L001-002` vence **hoy** (40 u.), `L001-003` vence **mañana** (60 u.), `L001-004` vigente (500 u.) | FEFO, exclusión de vencidos, alertas |
| MED-004 Penicilina | Stock normal | Alergia del paciente 70000001 |
| MED-009 Atorvastatina | Stock **igual** al mínimo (80) | Valor límite de la alerta de reposición |
| MED-015 Salbutamol | 5 unidades, mínimo 15 | Stock insuficiente, alerta |
| MED-026 Insulina | **Sin stock** | Alerta, primer ingreso de lote |
| MED-035 Ceftriaxona | Lotes que vencen en 15 y 29 días | Alerta por vencer (ventana de 30 días) |

Regla de alerta de reposición: el producto aparece cuando su stock **disponible** (lotes no vencidos) es **menor o igual** al mínimo.
Un lote que vence hoy todavía se dispensa hoy; no se puede **ingresar** un lote que vence hoy o antes.
