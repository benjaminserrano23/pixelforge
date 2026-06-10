# Documentación de PixelForge

Índice de docs internos del monorepo. La especificación funcional vive en
[`../PixelForge_Slice_Spec_1.md`](../PixelForge_Slice_Spec_1.md); estos
documentos cubren la implementación y operación.

## Por paso del roadmap

- **Paso 1 — Scaffold del monorepo + health endpoint**
  - [01-architecture.md](01-architecture.md) — visión arquitectónica del corte inicial: qué pieza vive dónde y por qué.
  - [02-run-guide.md](02-run-guide.md) — cómo arrancar el monorepo en Windows 11 (Docker y modo desarrollo).

- **Paso 2 — Auth con JWT y roles**
  - [04-auth.md](04-auth.md) — decisiones (jjwt vs resource-server, BCrypt, sin refresh, localStorage para Paso 3), endpoints, modelo de errores y smoke E2E con curl.

- **Transversal**
  - [03-workflow.md](03-workflow.md) — estrategia de ramas (main/develop/feature), verificación pre-producción y deploy con Vercel/Render.

## Por venir (a medida que avancemos)

- Paso 2 — Auth con JWT y roles (`PLAYER` / `DEVELOPER`).
- Paso 3 — Frontend auth + rutas protegidas.
- Paso 4 — CRUD de juegos + upload de portada.
- Paso 5 — Catálogo y detalle.
- Paso 6 — Adquisiciones, biblioteca y stats.
- Paso 7 — Pulido, diagramas (ER, clases, despliegue) y deploy.
