# Paso 6 — Adquisiciones, biblioteca y estadísticas

Entidad `Purchase` que conecta jugadores con juegos, biblioteca del jugador,
y dashboard de estadísticas del desarrollador con gráfico (Recharts).

## Estructura resultante

```
backend/src/main/java/com/pixelforge/app/purchase/
├── Purchase.java              # entidad; unique(user_id, game_id) evita duplicados
├── PurchaseRepository.java     # findByUserId, existsByUserIdAndGameId, statsByDeveloper (proyección)
├── GameStatsProjection.java    # proyección por interfaz para la query de stats
├── PurchaseService.java        # purchase, findLibrary, statsOverview
├── LibraryController.java      # GET /api/library (rol PLAYER)
├── StatsController.java        # GET /api/stats/overview (rol DEVELOPER)
├── dto/                        # PurchaseResponse, GameStatsEntry, StatsOverviewResponse
└── exception/AlreadyPurchasedException.java

frontend/src/
├── api/purchases.ts            # purchaseGame, fetchLibrary, fetchStatsOverview
├── pages/
│   ├── GameDetailPage.tsx      # botón "Adquirir" habilitado (antes deshabilitado)
│   ├── LibraryPage.tsx         # biblioteca del jugador
│   └── dev/StatsPage.tsx       # tarjetas de totales + gráfico de barras (Recharts, lazy)
```

`POST /api/games/{id}/purchase` vive en `GameController` (no en un
`PurchaseController` propio) porque semánticamente es una acción sobre un
juego, igual que `/cover`; `LibraryController` y `StatsController` sí son
recursos propios (`/api/library`, `/api/stats/overview`), no anidados bajo
`/games`.

## Decisiones

- **`amount` es un snapshot del precio, no una referencia.** Al comprar se
  copia `game.getPrice()` al momento de la transacción. Si el desarrollador
  sube o baja el precio después, los ingresos históricos en `/stats/overview`
  no deben cambiar retroactivamente — es lo que esperaría cualquier reporte
  financiero real.
- **Sin pasarela real (mock, según el spec).** "Adquirir" solo registra la
  fila de `Purchase`; no hay carrito, checkout ni proveedor de pago. La
  restricción de unicidad `(user_id, game_id)` a nivel de base de datos
  evita comprar el mismo juego dos veces sin necesitar un lock explícito.
- **Comprar un DRAFT es indistinguible de "no existe" (404), no un error
  distinto.** Mismo criterio que el catálogo público y que
  `findPublishedById` (Paso 4): no se filtra la existencia de juegos no
  publicados.
- **Proyección por interfaz para la query de stats**, no `select new` con un
  record. El soporte de Hibernate para construir records vía "select new" en
  JPQL es más frágil/reciente que las proyecciones de interfaz con alias,
  que llevan años siendo el patrón estable de Spring Data JPA.
- **El `left join` en `statsByDeveloper` incluye juegos sin ventas** (0
  adquisiciones, 0 ingresos) — el dashboard debe mostrar todos los juegos del
  desarrollador, no solo los que ya vendieron algo, para que un juego recién
  publicado sin compras aún aparezca en el gráfico con barra en cero.
- **Recharts con `React.lazy`.** La librería pesa ~150KB adicionales al
  bundle y solo la usa `/dev/stats`. Sin code-splitting, el catálogo público
  (que ve cualquier visitante sin sesión) pagaría ese peso en el bundle
  inicial sin necesitarlo. Con `lazy()` + `<Suspense>`, el bundle principal
  bajó de 622KB a 258KB; Recharts se descarga aparte (364KB) solo al entrar
  a estadísticas.

## Verificación

- **Backend**: 9 tests nuevos (`PurchaseServiceTest`, `LibraryControllerTest`,
  `StatsControllerTest`, más la extensión de `GameControllerTest` para el
  endpoint de compra) — 44 tests en total, verdes.
- **E2E manual con curl contra Postgres real** (2026-07-02): un DEVELOPER no
  puede comprar (403) · un PLAYER compra un juego publicado (200, `amount`
  igual al precio) · comprar de nuevo el mismo juego → 409 `already_purchased`
  · la biblioteca del comprador lo lista · un DEVELOPER no puede ver la
  biblioteca de otro (403, ruta es rol PLAYER) · `/api/stats/overview`
  agrega correctamente totales e ingresos · un PLAYER no puede ver stats
  (403, ruta es rol DEVELOPER).
- **E2E manual en navegador**: registro como PLAYER → detalle de juego
  publicado → botón "Adquirir" → pasa a "Ya está en tu biblioteca" → aparece
  en `/library` → login como DEVELOPER dueño → `/dev/stats` muestra tarjetas
  de totales y el gráfico de barras con el juego y sus adquisiciones. Sin
  errores en consola ni requests fallidos.
- `npm run build` sin errores de tipos; bundle principal reducido tras el
  lazy-loading de Recharts.

## Deuda asumida (se paga en el Paso 7)

- Sin tests automatizados de frontend todavía (Vitest + Testing Library).
- Sin diagramas (ER, clases, despliegue) ni deploy — llegan en el Paso 7,
  el último del roadmap.
