# Flujo de trabajo — ramas, verificación y deploy

Estrategia simplificada para un proyecto solo-dev de portafolio. Inspirada en
Git Flow, pero recortando lo que no aporta a una persona sola (sin
`release/*`, sin equipos paralelos).

## Ramas

| Rama | Rol | Quién puede mergear hacia ella |
|------|-----|--------------------------------|
| `main` | Refleja exactamente lo que está en producción. Siempre desplegable. | Solo desde `develop` (o `hotfix/*`) vía PR aprobado y verde |
| `develop` | Integración. Aquí confluyen las features mientras se prueban. | Desde `feature/*` vía PR |
| `feature/<nombre>` | Una rama por feature o bugfix. Vida corta. | — (se crea, se trabaja, se mergea, se elimina) |
| `hotfix/<nombre>` *(eventual)* | Parche urgente directo sobre producción. | Va a `main` y se rebajea a `develop` |

Convenciones de nombre:
- `feature/auth-jwt`, `feature/games-crud`, `feature/library-page`.
- `hotfix/health-endpoint-404`.

## Ciclo por feature

```bash
# 1. Partir de develop al día
git checkout develop
git pull

# 2. Crear la rama de feature
git checkout -b feature/<nombre-descriptivo>

# 3. Trabajar y commitear (Conventional Commits)
git add <archivos>
git commit -m "feat(auth): add JWT filter"
git push -u origin feature/<nombre>

# 4. Abrir PR feature -> develop en GitHub
#    - Revisar diff, verificar checks (cuando haya CI).
#    - Merge (squash recomendado para mantener develop con historia limpia).

# 5. Borrar la rama local y remota
git checkout develop && git pull
git branch -d feature/<nombre>
git push origin --delete feature/<nombre>
```

## Promoción a producción (develop → main)

Cuando `develop` está estable y verificado:

1. Abrir PR `develop` → `main`.
2. Verificar que **todos los checks** del paso de verificación están verdes.
3. Mergear (recomendado: **merge commit** para preservar la historia de develop).
4. *(Paso 7)* El merge a `main` dispara el deploy automático.

## Verificación pre-producción

Antes de mergear a `main`, exigir:

- **Backend**
  - Tests unitarios (`mvn test`) verdes.
  - Tests de integración con Postgres real (Testcontainers; lo añadimos en el
    Paso 6/7).
  - Build limpio: `mvn -DskipTests=false clean package`.
- **Frontend**
  - Type check (`tsc -b`).
  - Tests con Vitest + React Testing Library (lo añadimos en el Paso 3).
  - Build: `npm run build`.
- **End-to-end**
  - `docker compose up --build` arranca los 3 servicios.
  - Smoke test manual del flujo crítico del paso (ej. en Paso 2: register →
    login → llamada autenticada).
  - `GET /api/health` sigue respondiendo `UP`.
- **Más adelante (Paso 7)** todo esto correrá en GitHub Actions y el PR no
  podrá mergearse si falla un check.

## Protección de `main` (configurar en GitHub UI una sola vez)

`Settings → Branches → Branch protection rules → Add rule` para `main`:

- ✅ Require a pull request before merging
- ✅ Require approvals (puedes ponerlo en 0 si trabajas solo, o en 1 si añades
  a un revisor humano más adelante)
- ✅ Require status checks to pass before merging *(activar cuando exista CI)*
- ✅ Require branches to be up to date before merging
- ✅ Do not allow bypassing the above settings

> Esto evita los “oops, pusheé directo a main” que son el accidente clásico.

## Deploy (Paso 7)

| Pieza | Proveedor sugerido | Cómo trigger |
|---|---|---|
| Frontend (build estático de Vite) | **Vercel** | Push a `main` → production deploy; push a `develop` o cualquier `feature/*` con PR abierto → preview deploy con URL única |
| Backend (Spring Boot) | **Render** o **Railway** | Push a `main` → redeploy de la imagen Docker que ya tenemos en `backend/Dockerfile` |
| Postgres administrado | **Neon**, **Render Postgres** o **Railway Postgres** | Variables `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` configuradas en el panel; el backend ya las consume vía `${ENV:default}` desde el Paso 1 |

Preview deployments de Vercel + branch `develop` = puedes probar la app real
antes de mergear a `main`, con URL compartible si quieres feedback de
terceros.

## Excepción de bootstrap

El commit que añade este documento se hizo **directo en `main`** porque
todavía no existía `develop` cuando se escribió. A partir del siguiente
commit, todo cambio pasa por feature → develop → main.
