# Despliegue del staging

Staging = Hostinger (frontend) + Render (backend) + Neon (PostgreSQL). Solo datos sintéticos.

## 1. Base de datos en Neon

1. Crear un proyecto nuevo `csj-erp-staging` y copiar la cadena de conexión JDBC.
2. No hace falta crear tablas: Flyway las crea al arrancar el backend.

## 2. Backend en Render

1. New → Web Service → conectar el repositorio, **Root Directory** `backend`, runtime **Docker**.
2. Variables de entorno:

| Variable | Valor |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://<host-neon>/<db>?sslmode=require` |
| `DB_USER` / `DB_PASSWORD` | Credenciales de Neon |
| `JWT_SECRET` | Texto aleatorio de 40 o más caracteres |
| `CORS_ORIGINS` | URL del frontend en Hostinger, por ejemplo `https://erp.tudominio.com` |
| `SEED` | `demo` la primera vez (luego puede quedar, solo carga si la base está vacía) |
| `SEED_PASSWORD` | Contraseña de los usuarios demo (distinta de la de local) |

3. Health check path: `/actuator/health`.
4. **No** activar el perfil `test` en staging: expondría el endpoint de reinicio de datos.

El plan gratuito de Render suspende el servicio tras un rato sin tráfico. Antes de una demo o
sesión de usabilidad, abrir `/actuator/health` y esperar a que responda.

## 3. Frontend en Hostinger

1. Generar el sitio estático apuntando al backend de Render:

   ```bash
   cd frontend
   NEXT_PUBLIC_API_URL=https://<servicio>.onrender.com npm run build
   ```

2. Subir **el contenido** de `frontend/out/` (no la carpeta) a `public_html/` (o al subdominio) con el
   Administrador de archivos o FTP. Incluye un `.htaccess` con la página 404 y cabeceras de seguridad.
3. Cada cambio de frontend requiere repetir el build y la subida.

## Seguridad

- Ningún secreto se versiona: todo va en variables de entorno de Render o secretos de GitHub.
- Si alguna credencial llegó a un repositorio público, se rota (Neon permite regenerar la contraseña).
