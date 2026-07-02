# Documentación de PixelForge

Índice de docs internos del monorepo. Cubren las decisiones de implementación
y la operación día a día. El roadmap a alto nivel vive en el
[README raíz](../README.md).

## Por paso del roadmap

- **Paso 1 — Scaffold del monorepo + health endpoint**
  - [01-architecture.md](01-architecture.md) — visión arquitectónica del corte inicial: qué pieza vive dónde y por qué.
  - [02-run-guide.md](02-run-guide.md) — cómo arrancar el monorepo en Windows 11 (Docker y modo desarrollo).

- **Paso 2 — Auth con JWT y roles**
  - [04-auth.md](04-auth.md) — decisiones (jjwt vs resource-server, BCrypt, sin refresh, localStorage para Paso 3), endpoints, modelo de errores y smoke E2E con curl.

- **Paso 3 — Frontend auth y rutas protegidas**
  - [05-frontend-auth.md](05-frontend-auth.md) — cliente HTTP con JWT, AuthContext con estado loading, protección por rol con rutas anidadas y verificación E2E manual.

- **Paso 4 — Backend de juegos: CRUD, ownership y portadas**
  - [06-games-crud.md](06-games-crud.md) — entidad Game, catálogo público filtrado en SQL, ownership resuelto en el service, portadas en disco y dos bugs reales encontrados en la verificación E2E (cast de parámetros nulos en Postgres, permisos del volumen de uploads, AccessDeniedHandler faltante).

- **Paso 5 — Frontend de juegos: catálogo, detalle, mis juegos**
  - [07-games-frontend.md](07-games-frontend.md) — catálogo con filtros, formulario compartido crear/editar, subida de portada, y el bug real de la verificación E2E (editar un DRAFT daba 404 por reusar el endpoint público).

- **Transversal**
  - [03-workflow.md](03-workflow.md) — estrategia de ramas (main/develop/feature), verificación pre-producción y deploy con Vercel/Render.

## Por venir (a medida que avancemos)

- Paso 6 — Adquisiciones, biblioteca y stats.
- Paso 7 — Pulido, diagramas (ER, clases, despliegue) y deploy.
