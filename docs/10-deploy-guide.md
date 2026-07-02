# Guía de deploy — Paso 7

Esta guía la ejecuta el propio Benjamín: requiere cuentas y credenciales
personales (GitHub ya usado, más Vercel, Render/Railway y un Postgres
administrado) que un agente no puede crear ni autorizar en su nombre. El
repo ya tiene todo lo que el deploy necesita — Dockerfiles, variables de
entorno vía `${ENV:default}`, `vercel.json` con el rewrite de `/api`. Esta
guía es la lista de pasos manuales para conectar esas piezas.

## 1. Postgres administrado

Elegir **uno**: [Neon](https://neon.tech) (gratis, serverless) o el Postgres
administrado de Render/Railway (más simple si ya vas a usar esos paneles
para el backend).

1. Crear una base de datos nueva (nombre sugerido: `pixelforge`).
2. Copiar la connection string (formato `jdbc:postgresql://host:5432/pixelforge`)
   y el usuario/contraseña. Se usan en el paso 2.

## 2. Backend en Render (o Railway)

El repo ya tiene `backend/Dockerfile` (multi-stage, Maven → JRE) listo para
cualquier proveedor que soporte "deploy from Dockerfile".

En Render:

1. **New → Web Service** → conectar el repo `benjaminserrano23/pixelforge`.
2. **Root Directory**: `backend` (el Dockerfile está ahí, no en la raíz del monorepo).
3. **Environment**: Docker (Render detecta el Dockerfile automáticamente).
4. Variables de entorno (mismas que consume `application.properties`, ver `docs/04-auth.md` y `docs/06-games-crud.md`):

   | Variable | Valor |
   |---|---|
   | `SPRING_DATASOURCE_URL` | connection string del paso 1 |
   | `SPRING_DATASOURCE_USERNAME` | usuario de Postgres |
   | `SPRING_DATASOURCE_PASSWORD` | contraseña de Postgres |
   | `JWT_SECRET` | generar uno nuevo de 32+ caracteres — **no reusar** el de desarrollo (`dev-only-secret-...`) |
   | `JWT_EXPIRATION_MS` | `900000` (15 min, o el valor que prefieras) |
   | `PIXELFORGE_UPLOADS_DIR` | `/uploads` |

5. **Disco persistente**: agregar un volumen montado en `/uploads` (Render:
   "Disks" en la config del servicio). Sin esto, las portadas subidas se
   pierden en cada redeploy — mismo problema que resolvimos con el volumen
   Docker local en `docker-compose.yml` (`docs/06-games-crud.md`).
6. Deploy. Verificar `https://<tu-servicio>.onrender.com/api/health` → debe
   responder `{"status":"UP",...}`.
7. Guardar la URL del backend — se usa en el paso 3.

## 3. Frontend en Vercel

1. **Add New → Project** → conectar el mismo repo.
2. **Root Directory**: `frontend`.
3. Framework preset: Vite (Vercel lo detecta solo).
4. **Antes de desplegar**, editar `frontend/vercel.json` (ya está en el repo)
   y reemplazar `REEMPLAZAR-CON-TU-BACKEND.onrender.com` por la URL real del
   paso 2 en ambos rewrites (`/api/:path*` y `/uploads/:path*`).

   Por qué un `vercel.json` con rewrites y no una variable `VITE_API_URL`:
   en dev, nginx (`frontend/nginx.conf`) proxy-pasa `/api/` al backend por
   DNS interno de Docker; el código de React siempre llama a rutas
   relativas (`/api/...`) y nunca conoce el host del backend (decisión del
   Paso 1, ver `docs/01-architecture.md`). Los rewrites de Vercel preservan
   exactamente ese comportamiento en producción: el navegador sigue viendo
   same-origin, sin CORS que configurar en el backend.
5. Commitear el `vercel.json` editado y hacer push (o pegar la URL
   directamente en el dashboard de Vercel como regla de rewrite, si
   prefieres no commitear la URL de producción).
6. Deploy. Vercel da una URL tipo `https://pixelforge-<hash>.vercel.app`.

## 4. Verificación end-to-end en producción

Repetir el smoke test manual de cada paso, ahora contra las URLs reales:

1. Abrir el frontend de Vercel → catálogo carga (aunque esté vacío).
2. Registrar un DEVELOPER → crear juego → subir portada → publicar.
3. Registrar un PLAYER (otra pestaña/incógnito) → adquirir el juego.
4. `/dev/stats` del developer muestra la compra.
5. Revisar en las Network tools del navegador que las llamadas a `/api/**`
   devuelven 200 y no hay errores de CORS en consola.

## 5. Actualizar README y portfolio

Una vez verificado:

- Actualizar `README.md` (raíz de `pixelforge`) con las URLs reales de
  frontend y backend, reemplazando "Cómo correr" solo-Docker por un enlace
  a la demo en vivo.
- Actualizar `docs/03-workflow.md`: el "Deploy (Paso 7)" pasa de tabla
  propuesta a enlaces reales.
- Pasarle las URLs al portfolio (`benjaminserrano23/portfolio-benjamin-serrano`,
  `lib/projects.ts`) para linkear la demo en vivo del proyecto.

## Notas

- **CI ya corre en cada push** (`.github/workflows/ci.yml`): tests de
  backend y frontend, más el build de ambos. Vercel y Render redespliegan
  automáticamente en cada push a `main` independientemente de este CI (son
  pipelines separados) — si quieres bloquear el deploy cuando CI falla,
  actívalo como *required check* en la protección de rama de `main`
  (`docs/03-workflow.md`).
- Si `develop` vuelve a usarse activamente (ver excepción de bootstrap en
  `docs/03-workflow.md`), Vercel puede generar *preview deployments* por PR
  automáticamente — útil para revisar una feature antes de mergear a `main`.
