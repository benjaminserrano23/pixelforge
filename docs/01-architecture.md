# Paso 1 — Arquitectura del scaffold

> Estado: ✅ verificado el 2026-06-10 — `GET /api/health` devuelve
> `{"status":"UP","service":"pixelforge-backend","timestamp":"..."}` y el
> frontend lo renderiza en pantalla.

Este documento explica las decisiones arquitectónicas detrás del corte inicial
del monorepo (Paso 1 del roadmap del spec). El objetivo del paso no es entregar
una feature funcional, sino dejar montados los tres servicios y la cadena de
comunicación que el resto del proyecto va a usar.

## Topología

```
                      ┌─────────────────────────┐
   Navegador          │  pixelforge-frontend    │
   localhost:5173 ──► │  nginx :80   (build SPA)│
                      │                         │
                      │   /api/* (proxy_pass)   │
                      └────────────┬────────────┘
                                   │  red interna "default" de compose
                                   ▼
                      ┌─────────────────────────┐
                      │  pixelforge-backend     │
                      │  Spring Boot :8080      │
                      │   GET /api/health       │
                      └────────────┬────────────┘
                                   │
                                   ▼
                      ┌─────────────────────────┐
                      │  pixelforge-db          │
                      │  Postgres 14  :5432     │
                      └─────────────────────────┘
```

Tres contenedores, una red de Docker Compose. Cada servicio resuelve a los
demás por su **nombre de servicio** (`db`, `backend`, `frontend`) gracias al
DNS interno de Docker.

## Decisiones clave (y el "porqué")

### 1. Monorepo en carpetas hermanas, sin workspaces

Backend (JVM/Maven) y frontend (Node/npm) tienen ecosistemas distintos. Usar
workspaces de npm o un build system como Nx añadiría complejidad sin valor:
cada servicio ya tiene su propio gestor de dependencias maduro. La ventaja del
monorepo aquí es **versionar contrato y cliente en un solo commit**, no
compartir build.

### 2. Layout del backend por feature, no por capa

```
com.pixelforge.app/
├── PixelforgeApplication.java
├── config/            ← cross-cutting (SecurityConfig)
└── health/            ← feature: endpoint /api/health
```

En vez del clásico `controllers/services/repositories`, cada feature será su
propio paquete. Cuando el proyecto crezca a `auth/`, `games/`, `purchases/`,
`stats/` (pasos 2–6), cada uno tendrá su controller + service + repository +
DTOs dentro de su propio paquete. Acopla por dominio, no por tecnología.

### 3. Spring Security activo desde el primer commit

El spec exige Spring Security como dependencia (línea 4). En vez de añadirla
"a futuro", la dejé desde el Paso 1 con una configuración mínima en
[`SecurityConfig.java`](../backend/src/main/java/com/pixelforge/app/config/SecurityConfig.java):

- `requestMatchers("/api/**").permitAll()` — todo abierto por ahora.
- `csrf().disable()` — la API es stateless, no usamos sesiones con cookies.
- `sessionCreationPolicy(STATELESS)` — anticipa el modelo JWT del Paso 2.

Cuando llegue auth (Paso 2), este es el **único archivo** que se toca para
exigir token y verificar roles. Aislar la decisión de seguridad en un solo
sitio paga dividendos.

### 4. Configuración con env-vars + defaults

`application.properties` usa el patrón `${ENV:default}`:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/pixelforge}
```

- Por defecto apunta a `localhost:5432` → corre tal cual con `mvn spring-boot:run`
  y un Postgres local.
- En Compose, la env del contenedor `backend` sobrescribe a `jdbc:postgresql://db:5432/...`
  → el mismo binario corre dentro de Docker sin recompilar.

El backend nunca conoce su entorno: el entorno se lo dice.

### 5. Frontend: proxy `/api`, no CORS

`vite.config.ts` proxy-pasa `/api` a `localhost:8080`. `nginx.conf` hace lo
mismo en producción contra `backend:8080`. Resultado: el código React siempre
hace `fetch('/api/health')` sin host, y para el navegador todo es **mismo
origen**.

Alternativa que **no** elegí: habilitar CORS en el backend y que React llame
a `http://localhost:8080/api/...` con `Origin`. Esto funciona pero:
- Mezcla orígenes (cookies, OPTIONS preflight, debugging más confuso).
- Acopla el frontend a la URL absoluta del backend (un `.env` por entorno).

El proxy elimina los dos problemas: mismo path en dev y prod, sin CORS jamás.

### 6. Tailwind v4 sin `tailwind.config.js`

Tailwind 4 cambió el setup. Ya no se usa `tailwind.config.js` ni
`postcss.config.js`. La integración oficial es `@tailwindcss/vite`:

- Plugin registrado en `vite.config.ts`.
- En CSS basta con `@import "tailwindcss";` (una línea en
  [`src/index.css`](../frontend/src/index.css)).

Menos archivos, menos configuración que mantener.

### 7. Dockerfiles multi-stage

Backend (`backend/Dockerfile`):
1. Etapa `build`: Maven + JDK 17 compila el jar.
2. Etapa runtime: solo JRE 17 + el jar copiado. No Maven, no código fuente.
3. Corre como usuario `spring` no-root.

Frontend (`frontend/Dockerfile`):
1. Etapa `build`: Node 20 corre `npm run build`.
2. Etapa runtime: nginx alpine sirve `dist/` y proxy-pasa `/api`.

Imágenes finales **más pequeñas y con menos superficie de ataque** que si
arrastráramos las herramientas de build a producción.

### 8. Healthcheck en Postgres + `depends_on: service_healthy`

```yaml
db:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U pixelforge -d pixelforge"]
backend:
  depends_on:
    db:
      condition: service_healthy
```

Sin esto, el backend a veces gana la carrera al arranque y muere con
"connection refused" porque Postgres aún no acepta conexiones. `service_healthy`
espera al `pg_isready`, no al PID.

### 9. Volumen `db-data` nombrado, no bind mount

Volumen gestionado por Docker. Persiste entre `docker compose down/up` y no
sufre problemas de permisos en Windows que sí ocurrirían con un bind mount a
`C:\Users\...`.

## Lo que el Paso 1 deja listo para el resto del roadmap

- Una pieza donde colgar entidades JPA (la conexión a Postgres ya funciona).
- Un punto único para activar autenticación (`SecurityConfig`).
- Un patrón de proxy que el frontend reusará para todos los endpoints (no solo `/api/health`).
- Un docker-compose listo para sumar más servicios (Redis, MinIO para upload de imágenes…) si hicieran falta.
