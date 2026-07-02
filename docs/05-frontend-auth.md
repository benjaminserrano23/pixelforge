# Paso 3 — Frontend auth

Login/register en la SPA, sesión global y rutas protegidas por rol. Cierra el
ciclo del Paso 2: el JWT que emite el backend ahora tiene un consumidor real.

## Estructura resultante

```
frontend/src/
├── main.tsx              # árbol de rutas (React Router 7)
├── types.ts              # tipos espejo de los DTOs del backend
├── api/
│   ├── client.ts         # fetch wrapper: JWT, JSON, errores tipados (ApiError)
│   └── auth.ts           # register / login / me
├── context/
│   └── AuthContext.tsx   # sesión global (user, loading, login/register/logout)
├── components/
│   ├── Layout.tsx        # navbar según sesión y rol + <Outlet/>
│   ├── ProtectedRoute.tsx# guarda por sesión y rol
│   └── FormField.tsx     # input con label + error por campo
└── pages/
    ├── HomePage.tsx      # placeholder de catálogo (Paso 5)
    ├── LoginPage.tsx
    ├── RegisterPage.tsx
    ├── HealthPage.tsx    # ex-App.tsx, conservado en /health para el smoke test
    └── PlaceholderPage.tsx # /library, /dev/games, /dev/stats (Pasos 5-6)
```

## Decisiones

- **JWT en localStorage** (`pixelforge_token`), como se decidió en
  [04-auth.md](04-auth.md). El "interceptor" es el wrapper `api()` de
  `client.ts`: toda petición sale con `Authorization: Bearer` si hay token.
  Un 401 con token guardado significa token expirado → se limpia ahí mismo,
  para que ninguna capa superior intente restaurar una sesión muerta.
- **Errores tipados, no strings.** `client.ts` convierte respuestas no-2xx en
  `ApiError { status, body }` con el cuerpo del `GlobalExceptionHandler`
  (`validation_failed` + `fields`, `invalid_credentials`,
  `email_already_used`). Las páginas deciden el mensaje: 401 → "email o
  contraseña incorrectos", 409 → "email ya registrado", 400 → error bajo cada
  campo del formulario.
- **`AuthContext` con estado `loading`.** Al recargar con token guardado, el
  contexto valida contra `/api/auth/me` antes de decidir. Sin ese estado
  intermedio, `ProtectedRoute` redirigiría a /login durante el instante en que
  `/me` aún no respondió (flash de "no autenticado").
- **Protección por rol como rutas anidadas.** `<ProtectedRoute role="...">`
  envuelve grupos de rutas con `<Outlet/>`: sin sesión → /login (recordando
  `from` para volver tras el login); rol equivocado → home. Los placeholders
  de /library y /dev/* existen desde ya precisamente para poder verificar esta
  lógica antes de construir su UI real.
- **La UI de roles es cosmética, la autoridad es el backend.** La navbar
  muestra u oculta links según el rol, pero la barrera real siguen siendo las
  reglas de `SecurityConfig` — el frontend solo evita enseñar puertas que el
  backend cerraría.

## Verificación E2E (manual, 2026-07-02)

Con `docker compose up db backend` + `npm run dev` en `frontend/`:

1. Registro como DEVELOPER → redirige a /dev/games, navbar muestra nombre y rol. ✔
2. Recarga en frío → sesión restaurada vía /me (sin flash de logout). ✔
3. DEVELOPER navega a /library → redirigido a home (rol equivocado). ✔
4. Logout → token eliminado; /dev/games → redirigido a /login. ✔
5. Login con contraseña mala → alerta "Email o contraseña incorrectos". ✔
6. Login correcto → vuelve a /dev/games (el `from` guardado en el state). ✔
7. Consola del navegador sin errores ni warnings. ✔

## Deuda asumida (se paga en pasos siguientes)

- Sin refresh token: a los 15 min el token expira y el siguiente request
  protegido limpia la sesión; el usuario vuelve a loguearse (decisión de
  04-auth.md).
- Tests automatizados del frontend llegan en el Paso 7 (Vitest + Testing
  Library sobre AuthContext y formularios).
