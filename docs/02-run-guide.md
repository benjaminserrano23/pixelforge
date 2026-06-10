# Cómo correr PixelForge (Windows 11 / PowerShell)

Dos modos:

- **Opción A — Todo en Docker.** Lo más simple para verificar que el corte
  funciona de punta a punta.
- **Opción B — Modo desarrollo.** Solo Postgres en Docker; backend y frontend
  nativos para tener hot reload.

## Requisitos (instalar una sola vez)

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install OpenJS.NodeJS.LTS
winget install Docker.DockerDesktop
winget install Apache.Maven
winget install Git.Git
```

Docker Desktop pide reiniciar la primera vez y necesita que esté **abierto y
corriendo** antes de usar `docker compose`.

Verificación:

```powershell
java -version    # 17.x
node -v          # >= 20
npm -v
mvn -v           # 3.9.x
docker --version
```

> **Nota:** el scaffold no incluye Maven wrapper (`mvnw.cmd`), por eso necesitas
> Maven instalado globalmente para la Opción B. Si más adelante incomoda, se
> añade el wrapper con `mvn -N wrapper:wrapper` desde `backend/`.

## Opción A — Todo en Docker

Desde la carpeta que contiene `pixelforge/`:

```powershell
cd pixelforge
docker compose up --build
```

La primera vez tarda varios minutos (descarga imágenes base y compila el
backend). Después abre:

- Frontend: <http://localhost:5173> — tarjeta verde con `Estado: UP`.
- Backend directo: <http://localhost:8080/api/health> — JSON crudo.
- Postgres: `localhost:5432` (usuario `pixelforge`, password `pixelforge`, db `pixelforge`).

Para apagar:

```powershell
docker compose down       # quita contenedores, conserva el volumen db-data
# docker compose down -v  # añade -v para borrar también los datos
```

## Opción B — Modo desarrollo (hot reload)

Necesitarás **tres terminales** de PowerShell.

### Terminal 1 — Postgres en background

```powershell
cd pixelforge
docker compose up -d db
```

El servicio Postgres se llama `db` (no `postgres`). Verifica:

```powershell
docker compose ps
docker compose logs db --tail 20
```

### Terminal 2 — Backend Spring Boot

```powershell
cd pixelforge\backend
mvn spring-boot:run
```

Espera a `Started PixelforgeApplication in X seconds`. Como
`application.properties` toma `localhost:5432` por defecto, conecta solo al
Postgres en Docker. Smoke test:

```powershell
curl http://localhost:8080/api/health
```

### Terminal 3 — Frontend Vite

```powershell
cd pixelforge\frontend
npm install          # solo la primera vez o tras cambiar deps
npm run dev
```

Abre <http://localhost:5173>. Vite proxy-pasa `/api/*` a `http://localhost:8080`
automáticamente — **no hace falta crear `.env`**, el proxy ya está en
[`vite.config.ts`](../frontend/vite.config.ts).

### Apagar el modo desarrollo

`Ctrl+C` en las terminales 2 y 3, y luego:

```powershell
docker compose down
```

## Troubleshooting (con los nombres reales del scaffold)

| Síntoma | Causa probable | Solución |
|---|---|---|
| `docker compose up` se queda colgado en `db` y no arranca el backend | Puerto 5432 ocupado por otro Postgres en el host | `Stop-Service postgresql*` o quita el mapeo `"5432:5432"` del compose |
| Backend cae con `Connection refused` al arrancar | Olvidaste levantar `db` antes | `docker compose up -d db` y reintenta |
| Frontend tarjeta roja con `HTTP 401`/`403` | Algo se rompió en `SecurityConfig` | Verifica que `requestMatchers("/api/**").permitAll()` siga ahí |
| Frontend tarjeta roja con `Failed to fetch` | Backend caído o proxy mal | `curl http://localhost:8080/api/health` desde otra terminal |
| `mvn` no se reconoce | No instalaste Maven o no reabriste PowerShell | Reabre la terminal tras `winget install Apache.Maven` |
| `npm install` falla con `EACCES` o `EBUSY` | Antivirus o OneDrive bloquean `node_modules` | Mueve el proyecto fuera de carpetas sincronizadas |
