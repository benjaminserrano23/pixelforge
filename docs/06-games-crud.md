# Paso 4 — Backend de juegos: CRUD, ownership y portadas

Entidad `Game` con catálogo público, CRUD para el desarrollador dueño y
subida de portada a disco. Es la primera vez que el backend tiene datos
con dueño (a diferencia de `User`, que es dueño de sí mismo).

## Estructura resultante

```
backend/src/main/java/com/pixelforge/app/game/
├── Game.java                  # entidad: título, precio, status, developer (FK)
├── GameStatus.java            # DRAFT | PUBLISHED
├── GameRepository.java        # findByDeveloperId, findPublished (filtro genre/search)
├── GameService.java            # reglas de negocio y ownership
├── GameController.java         # mapeo HTTP
├── CoverStorageService.java     # guarda portadas en disco, valida tipo/tamaño
├── dto/                        # GameRequest, GameResponse, PageResponse<T>
└── exception/                  # GameNotFoundException, NotGameOwnerException
```

## Decisiones

- **Un juego siempre nace `DRAFT`.** `GameService.create` ignora el `status`
  del request de entrada; publicarlo es una acción explícita posterior vía
  `PUT` con `status=PUBLISHED`. Evita que un alta accidental exponga un
  juego a medio terminar en el catálogo público.
- **Ownership se resuelve en el service, no con `@PreAuthorize`.** El rol
  (`DEVELOPER`) se valida en `SecurityConfig` (autorización gruesa); *cuál*
  desarrollador puede tocar *cuál* juego es una regla de negocio con datos
  (comparar `developer.id`), así que vive en `GameService.requireOwnedGame`.
  Mezclar ambos niveles en anotaciones de método habría sido más difícil de
  testear con Mockito puro.
- **`NotGameOwnerException` → 403, no 404.** A diferencia de auth (donde se
  oculta si un email existe), aquí no hay nada sensible que proteger
  ocultando la existencia del juego: un 403 explícito es más útil para
  depurar que un 404 genérico.
- **El catálogo público filtra por `status = PUBLISHED` en la query, no en
  memoria.** `GameRepository.findPublished` limita el filtro directamente en
  SQL con parámetros opcionales de género/búsqueda. Un DRAFT nunca sale de
  la base hacia una respuesta pública, incluso pidiendo su id directamente
  (`findPublishedById` trata "existe pero es DRAFT" igual que "no existe").
- **Cast explícito de parámetros nulos en JPQL (bug real encontrado en
  verificación E2E).** `lower(:search)` con `:search = null` funciona en H2
  (los tests iniciales pasaban) pero falla contra Postgres real con
  `function lower(bytea) does not exist`: Postgres no puede inferir el tipo
  de un parámetro sin contexto de columna cuando está envuelto en una
  función. Se resolvió con `cast(:search as string)`. Lección: los tests con
  H2 no sustituyen una verificación E2E contra el motor real.
- **Portadas en disco, no en la base.** `CoverStorageService` valida tipo
  (`png`/`jpeg`/`webp`) y tamaño (5 MB) antes de guardar con nombre `UUID` en
  `pixelforge.uploads.dir`, servido como estático vía `WebConfig` bajo
  `/uploads/**`. En Docker es un volumen nombrado (`uploads-data`) montado en
  el contenedor `backend`; migrar a S3/Cloud Storage sería un cambio interno
  de esa única clase.
- **`AccessDeniedHandler` explícito (bug real encontrado en verificación
  E2E).** Sin él, un usuario autenticado sin el rol requerido (ej. un
  `PLAYER` intentando `POST /api/games`) recibía **401** del
  `authenticationEntryPoint` en vez de **403** — Spring Security puede
  enrutar `AccessDeniedException` al entry point de autenticación según el
  estado de confianza del `Authentication`. Se agregó `JwtAccessDeniedHandler`
  para forzar 403 con el mismo formato JSON que el resto de errores de la
  API (`{"error": "forbidden"}`).
- **Volumen de uploads con dueño correcto (bug real encontrado en
  verificación E2E).** El contenedor corre como usuario no-root (`spring`,
  Dockerfile del Paso 1). Un volumen nombrado vacío se monta con dueño
  `root`, así que la primera subida fallaba con
  `java.nio.file.AccessDeniedException`. Se resolvió creando `/uploads` con
  `chown spring:spring` en el Dockerfile *antes* de `USER spring`: Docker
  copia el contenido y permisos de ese directorio al volumen la primera vez
  que se monta si está vacío.

## Verificación

- **Tests automatizados**: 32 verdes (`GameServiceTest` con Mockito cubre
  create/update/delete/ownership/catálogo; `GameControllerTest` con MockMvc
  cubre el mapeo HTTP incluyendo validación 400 y errores 403/404).
- **E2E manual contra Postgres real** (2026-07-02), con `docker compose up
  db backend`:
  1. Crear juego → `DRAFT`, no aparece en catálogo público. ✔
  2. Publicar (`PUT` con `status=PUBLISHED`) → aparece en catálogo. ✔
  3. Filtro por género y por texto en el catálogo. ✔
  4. Subir portada (PNG) → URL servida y descargable con `content-type:
     image/png`. ✔
  5. Otro `DEVELOPER` intenta editar/borrar → 403 `not_game_owner`. ✔
  6. `PLAYER` intenta crear un juego → 403 `forbidden`. ✔
  7. El dueño real borra su juego → 200, desaparece del catálogo. ✔

## Deuda asumida (se paga en pasos siguientes)

- Sin paginación configurable desde el frontend todavía (Paso 5 la
  consumirá vía `PageResponse`).
- La subida de portada no genera variantes/thumbnails; sirve el archivo tal
  cual se subió.
