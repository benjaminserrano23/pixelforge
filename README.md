# PixelForge

Plataforma de publicación de videojuegos. Corte vertical reconstruido de cero
para portafolio, inspirado en el proyecto universitario UFRO GameLab.

**Stack:** React 19 + TypeScript + Vite + Tailwind v4 · Spring Boot 3
(JDK 17, Maven) · PostgreSQL 14 · JWT · Docker.

## Estado del roadmap

| Paso | Descripción | Estado |
|------|-------------|--------|
| 1 | Scaffold monorepo + `docker-compose` + endpoint `/api/health` | ✅ verificado 2026-06-10 |
| 2 | Backend auth: User + register/login + JWT + Spring Security con roles | ✅ implementado 2026-06-10 (15 tests verde) |
| 3 | Frontend auth: login/register + AuthContext + rutas protegidas | ⏳ siguiente |
| 4 | Backend juegos: entidad Game + CRUD + ownership + upload imagen | — |
| 5 | Frontend juegos: catálogo, detalle, mis juegos, form crear/editar | — |
| 6 | Adquisiciones + biblioteca + endpoint stats + dashboard con gráfico | — |
| 7 | Pulido, diagramas (ER/clases/despliegue), tests, deploy | — |

## Estructura

```
pixelforge/
├── backend/                              # Spring Boot 3, Maven, JDK 17
│   ├── pom.xml                           # jjwt, postgres, spring-boot starters
│   ├── Dockerfile                        # multi-stage: maven build -> jre runtime
│   └── src/
│       ├── main/java/com/pixelforge/app/
│       │   ├── PixelforgeApplication.java
│       │   ├── auth/                     # AuthController, AuthService, dto/, jwt/, exception/
│       │   ├── config/                   # SecurityConfig (filtro JWT integrado)
│       │   ├── health/                   # HealthController (/api/health)
│       │   └── user/                     # User entity, UserRole, UserRepository
│       ├── main/resources/application.properties
│       └── test/java/com/pixelforge/app/ # AuthServiceTest, AuthControllerTest, UserRepositoryTest
├── frontend/                             # React 19 + Vite + TS + Tailwind v4
│   ├── package.json, vite.config.ts, tsconfig.json
│   ├── nginx.conf, Dockerfile            # multi-stage: node build -> nginx runtime
│   ├── index.html
│   └── src/                              # main.tsx, App.tsx, index.css
├── docs/                                 # documentación interna
│   ├── 01-architecture.md                # decisiones del Paso 1
│   ├── 02-run-guide.md                   # cómo arrancar el monorepo (Windows)
│   ├── 03-workflow.md                    # ramas, verificación, deploy
│   └── 04-auth.md                        # decisiones del Paso 2
├── docker-compose.yml                    # db + backend + frontend
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
- Spring Security activo desde el primer commit. En el Paso 1 abría `/api/**`
  para no bloquear el smoke test; en el Paso 2 ese mismo `SecurityConfig` se
  reescribió para exigir JWT en todas las rutas salvo `/api/health` y
  `/api/auth/{register,login}`, y validar roles `PLAYER` / `DEVELOPER`.
- Configuración del backend con `${ENV:default}`: el mismo jar corre con
  `localhost` en local y `db` dentro de Compose, sin recompilar.

## Flujo de trabajo

- Ramas: `main` (producción), `develop` (integración), `feature/<nombre>` por feature.
- Cada feature se mezcla a `develop` vía Pull Request. Cuando `develop` está estable y verificado, se promueve a `main` vía PR.
- Detalles, comandos paso a paso y reglas de protección en [`docs/03-workflow.md`](docs/03-workflow.md).

## Repositorio remoto

<https://github.com/benjaminserrano23/pixelforge>

## Trabajo futuro (fuera del MVP)

Conscientemente recortado del alcance inicial:

- Pasarela de pago real (las adquisiciones del MVP son mock).
- Chat entre usuarios y sistema de amigos.
- Juegos embebidos jugables en el navegador (vía Phaser u otro engine).
- Ranking / leaderboard cross-juegos.

Recortar alcance temprano es una decisión de ingeniería: priorizar que el
corte vertical (publicar → descubrir → adquirir → estadísticas) funcione
bien antes que cubrir todas las features posibles a medias.
