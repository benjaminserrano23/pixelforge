# PixelForge

Plataforma de publicación de videojuegos. Corte vertical reconstruido de cero
para portafolio, inspirado en el proyecto universitario UFRO GameLab.

**Stack:** React 19 + TypeScript + Vite + Tailwind v4 · Spring Boot 3
(JDK 17, Maven) · PostgreSQL 14 · JWT · Docker.

> Spec funcional completo en [`PixelForge_Slice_Spec_1.md`](PixelForge_Slice_Spec_1.md).

## Estado del roadmap

| Paso | Descripción | Estado |
|------|-------------|--------|
| 1 | Scaffold monorepo + `docker-compose` + endpoint `/api/health` | ✅ verificado 2026-06-10 |
| 2 | Backend auth: User + register/login + JWT + Spring Security con roles | ⏳ siguiente |
| 3 | Frontend auth: login/register + AuthContext + rutas protegidas | — |
| 4 | Backend juegos: entidad Game + CRUD + ownership + upload imagen | — |
| 5 | Frontend juegos: catálogo, detalle, mis juegos, form crear/editar | — |
| 6 | Adquisiciones + biblioteca + endpoint stats + dashboard con gráfico | — |
| 7 | Pulido, diagramas (ER/clases/despliegue), tests, deploy | — |

## Estructura

```
pixelforge/
├── backend/                       # Spring Boot 3, Maven, JDK 17
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/pixelforge/app/
│       │   ├── PixelforgeApplication.java
│       │   ├── config/SecurityConfig.java
│       │   └── health/HealthController.java
│       └── resources/application.properties
├── frontend/                      # React 19 + Vite + TS + Tailwind v4
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── nginx.conf
│   ├── Dockerfile
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       └── index.css
├── docs/                          # documentación interna
│   ├── 01-architecture.md
│   └── 02-run-guide.md
├── docker-compose.yml             # db + backend + frontend
├── PixelForge_Slice_Spec_1.md     # spec funcional
└── README.md
```

## Cómo correr

Guía completa en [`docs/02-run-guide.md`](docs/02-run-guide.md). Resumen:

```bash
cd pixelforge
docker compose up --build
```

- Frontend: <http://localhost:5173>
- Backend:  <http://localhost:8080/api/health>
- Postgres: `localhost:5432` (usuario `pixelforge`, password `pixelforge`, db `pixelforge`)

## Decisiones arquitectónicas

Resumidas en [`docs/01-architecture.md`](docs/01-architecture.md). Lo
fundamental:

- Backend y frontend cohabitan el monorepo, pero cada uno mantiene su gestor
  de dependencias y su `Dockerfile`.
- El frontend nunca conoce la URL del backend: hace `fetch('/api/health')`
  y un proxy (Vite en dev, nginx en prod) lo enruta. No usamos CORS.
- Spring Security activo desde el primer commit; en este paso permite todo
  `/api/**` para no bloquear el smoke test. En el Paso 2 ese mismo archivo
  pasa a exigir JWT y validar roles.
- Configuración del backend con `${ENV:default}`: el mismo jar corre con
  `localhost` en local y `db` dentro de Compose, sin recompilar.

## Flujo de trabajo

- Ramas: `main` (producción), `develop` (integración), `feature/<nombre>` por feature.
- Cada feature se mezcla a `develop` vía Pull Request. Cuando `develop` está estable y verificado, se promueve a `main` vía PR.
- Detalles, comandos paso a paso y reglas de protección en [`docs/03-workflow.md`](docs/03-workflow.md).

## Repositorio remoto

<https://github.com/benjaminserrano23/pixelforge>

## Trabajo futuro (fuera del MVP)

Documentado en el spec, sección 1: pasarela de pago Flow, chat/amigos, juegos
embebidos en Phaser, ranking. Mostrar que se sabe recortar alcance es criterio
de ingeniería explícito del proyecto.
