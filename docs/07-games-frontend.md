# Paso 5 — Frontend de juegos: catálogo, detalle, mis juegos

Consumo real de la API del Paso 4: catálogo público con filtros, detalle,
gestión de "mis juegos" (publicar/despublicar/borrar) y formulario de
crear/editar con subida de portada.

## Estructura resultante

```
frontend/src/
├── api/games.ts             # fetchCatalog, fetchGame, fetchOwnedGame, fetchMyGames,
│                             # createGame, updateGame, deleteGame, uploadCover
├── components/GameCard.tsx  # tarjeta reusada en catálogo y "mis juegos"
├── pages/
│   ├── HomePage.tsx         # ahora es el catálogo real (antes placeholder)
│   ├── GameDetailPage.tsx   # detalle público; botón "Adquirir" deshabilitado (Paso 6)
│   └── dev/
│       ├── MyGamesPage.tsx  # lista propia + publicar/despublicar + borrar
│       └── GameFormPage.tsx # un solo form para crear y editar + subida de portada
```

## Decisiones

- **Sin debounce en la búsqueda del catálogo.** El volumen esperado (un
  portafolio, no un e-commerce real) no justifica la complejidad extra; cada
  tecleo dispara un fetch liviano a `/api/games`.
- **Un formulario para crear y editar** (`GameFormPage`), distinguido solo
  por si `id` viene en la URL. Repetir los mismos campos en dos componentes
  no habría aportado nada.
- **La portada solo puede subirse en modo edición.** El backend la asocia a
  un juego que ya existe (`POST /api/games/{id}/cover`), así que tras crear
  un juego se navega directo a `/dev/games/:id/edit` en vez de a la lista —
  para que el usuario pueda subirla en el mismo flujo sin un paso extra.
- **`apiUpload` separado de `api` en el cliente HTTP.** Subir un archivo
  necesita `FormData` sin `Content-Type` manual (el navegador debe fijar el
  boundary del multipart); forzar el mismo wrapper para ambos casos habría
  significado ramificar `api()` con un flag. Ambas funciones comparten el
  manejo de respuesta (`handleResponse`) para no duplicar el mapeo de errores.
- **"Despublicar" y "Publicar" reenvían el juego completo.** El backend
  valida el DTO entero en cada `PUT` (no hay `PATCH` parcial), así que
  `togglePublish` en `MyGamesPage` reenvía los campos actuales del juego con
  solo el `status` invertido, en vez de introducir un endpoint dedicado para
  un cambio de un solo campo.

## Bug real encontrado en la verificación E2E: editar un DRAFT daba 404

El primer intento de `GameFormPage` reusaba `fetchGame` (el mismo endpoint
público del catálogo, `GET /api/games/{id}`) para precargar el formulario de
edición. Pero ese endpoint solo expone juegos `PUBLISHED` — y un juego recién
creado siempre nace `DRAFT` (ver `06-games-crud.md`). Resultado observado en
`preview_network`: `GET /api/games/4 → 404 Not Found` justo después de crear
el juego y aterrizar en su página de edición.

**Fix**: se agregó un endpoint separado con semántica distinta —
`GET /api/games/mine/{id}` (`GameService.findOwned`, mismo chequeo de
ownership que `update`/`delete`/`uploadCover`) — que devuelve el juego del
desarrollador autenticado sin importar su `status`. El frontend usa
`fetchGame` solo para el detalle público y `fetchOwnedGame` solo para
precargar el formulario de edición. Se agregaron 2 tests de service y 1 de
controller cubriendo el caso.

## Verificación E2E manual (navegador, 2026-07-02)

Con `docker compose up db backend` + `npm run dev`:

1. Registro como DEVELOPER → "Mis juegos" vacío. ✔
2. Crear juego → aterriza en `/dev/games/:id/edit` con el DRAFT cargado
   correctamente (antes del fix: 404). ✔
3. Subir portada (simulando selección de archivo con `DataTransfer`) →
   imagen visible tras la subida y tras recargar la página. ✔
4. Publicar desde "Mis juegos" → aparece en el catálogo. ✔
5. Buscar por texto en el catálogo → filtra correctamente. ✔
6. Abrir el detalle público del juego → muestra portada, precio,
   descripción; botón "Adquirir" deshabilitado con tooltip al Paso 6. ✔
7. `npm run build` (`tsc -b && vite build`) sin errores de tipos. ✔
8. 35 tests de backend en verde (2 nuevos en `GameServiceTest`, 1 en
   `GameControllerTest` para `findOwned`/`mineDetail`).

## Deuda asumida (se paga en el Paso 6)

- Sin tests automatizados de frontend todavía (llegan en el Paso 7 con
  Vitest + Testing Library).
- `/library` y `/dev/stats` siguen como placeholders.
