# Paso 2 — Autenticación con JWT y roles

> Estado: ✅ backend implementado, 15 tests verdes, smoke E2E verificado.
> El frontend de auth (login/register + `AuthContext` + rutas protegidas) llega en el Paso 3.

## Decisiones arquitectónicas

### 1. JWT propio (jjwt + filtro custom), no `oauth2-resource-server`

Spring Security trae `spring-boot-starter-oauth2-resource-server` que hace validación de JWT lista para usar. **No lo uso.** Razones:

- Sigue siendo necesario un emisor de tokens propio (resource-server solo valida, no emite).
- El control explícito sobre claims (`uid`, `role`) y verificación es didáctico — entender el mecanismo es parte del valor de este proyecto.
- jjwt 0.12.x es la librería de referencia en tutoriales modernos de Spring Boot 3.

### 2. Una sola access token, sin refresh

- Expiración corta: **15 minutos** (`pixelforge.jwt.expiration-ms=900000`).
- No hay refresh token en MVP. Cuando expira, el frontend redirige a login.
- Si más adelante notamos UX dolorosa, agregamos refresh en Paso 7 (deploy/pulido).

### 3. `BCryptPasswordEncoder` (cost por defecto 10)

- Estándar en Spring Security.
- Hash lento con salt: protege contra rainbow tables incluso si la DB se filtra.
- Cost 10 en 2026 es razonable; subir a 12 si hace falta más adelante.

### 4. Secreto JWT por env-var con default de dev

```properties
pixelforge.jwt.secret=${JWT_SECRET:dev-only-secret-change-me-in-production-please-pixelforge}
```

- En dev / Docker Compose: secreto fijo embebido (suficiente para localhost).
- En producción (Render/Railway en Paso 7): inyectado vía panel del proveedor.
- Validación al arrancar: si tiene <32 bytes (mínimo HMAC-SHA256), la app falla rápido y claro.

### 5. Estructura del paquete por feature

```
com.pixelforge.app/
├── user/
│   ├── User.java                      ← @Entity, tabla "users" (singular es reservada en PG)
│   ├── UserRole.java                  ← enum PLAYER | DEVELOPER
│   └── UserRepository.java
└── auth/
    ├── AuthController.java            ← /api/auth/{register,login,me}
    ├── AuthService.java               ← lógica de registro, login, /me
    ├── dto/
    │   ├── RegisterRequest.java       ← record con @Email, @Size, @NotNull (Bean Validation)
    │   ├── LoginRequest.java
    │   ├── AuthResponse.java          ← {token, user}
    │   └── UserResponse.java          ← sin passwordHash (NUNCA se devuelve)
    ├── jwt/
    │   ├── JwtService.java            ← firma + parse + validación de claims
    │   └── JwtAuthenticationFilter.java ← OncePerRequestFilter
    └── exception/
        ├── EmailAlreadyUsedException.java
        └── GlobalExceptionHandler.java ← @RestControllerAdvice (400/401/409)
```

`SecurityConfig` queda en `config/` porque es cross-cutting (no específico de auth).

### 6. Storage del token en frontend: `localStorage` (Paso 3)

| Opción | Pros | Contras |
|---|---|---|
| **`localStorage`** | Fácil de leer desde JS, sin CSRF | Vulnerable a XSS si un script inyectado lo lee |
| `httpOnly cookie` | Inmune a XSS | Requiere CSRF token y configurar `SameSite`; más fricción |

Para este MVP de portafolio elegimos **`localStorage`**. Lo hacemos explícito en el código y aceptamos el trade-off: la aplicación no maneja datos sensibles más allá del propio JWT, y la superficie XSS está acotada (React escapa por defecto, no usamos `dangerouslySetInnerHTML`).

En el Paso 7, al hacer el deploy real, evaluamos migrar a `httpOnly cookie` si el contexto lo amerita.

## Endpoints

| Verbo | Path | Auth | Body request | Body response | Códigos |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | público | `{email, password, displayName, role}` | `{token, user}` | 200 / 400 / 409 |
| POST | `/api/auth/login` | público | `{email, password}` | `{token, user}` | 200 / 400 / 401 |
| GET  | `/api/auth/me`       | Bearer JWT | — | `UserResponse` | 200 / 401 |
| GET  | `/api/health`        | público | — | `{status, service, timestamp}` | 200 |

Forma del JWT (claims):

```json
{
  "sub": "anna@example.com",
  "uid": 1,
  "role": "PLAYER",
  "iat": 1718000000,
  "exp": 1718000900
}
```

Firma: HMAC-SHA256.

## Cómo se conecta el JWT al `SecurityContext`

1. Request llega con `Authorization: Bearer <token>`.
2. `JwtAuthenticationFilter` (extiende `OncePerRequestFilter`) extrae el token.
3. `JwtService.parse(token)` valida firma y expiración. Si falla → `JwtException` capturada → contexto limpio.
4. Si valida: se crea un `UsernamePasswordAuthenticationToken` con el email como principal y `ROLE_<UserRole>` como authority.
5. Se inserta en `SecurityContextHolder`.
6. La cadena de Spring Security ve un usuario autenticado y permite el acceso a `/api/auth/me`.

En `AuthController.me(Authentication auth)`, Spring inyecta el `Authentication` desde el `SecurityContext`, y leemos `auth.getName()` = email del subject del JWT.

## Modelo de errores

`GlobalExceptionHandler` traduce excepciones a respuestas JSON consistentes:

| Excepción | HTTP | Body |
|---|---|---|
| `MethodArgumentNotValidException` (Bean Validation falla) | 400 | `{"error":"validation_failed","fields":{"email":"must be a well-formed email address",...}}` |
| `BadCredentialsException` | 401 | `{"error":"invalid_credentials"}` |
| `EmailAlreadyUsedException` | 409 | `{"error":"email_already_used"}` |

**Login**: tanto email desconocido como password incorrecto devuelven el mismo `invalid_credentials` para no filtrar existencia de cuentas.

## Tests (15 total, 100% verde)

```
AuthServiceTest      7  (Mockito puro)
UserRepositoryTest   3  (@DataJpaTest + H2 en memoria)
AuthControllerTest   5  (@WebMvcTest + MockMvc + @MockBean)
```

Cobertura:
- Service: register success / email duplicado, login OK / password malo / email desconocido, me OK / user borrado.
- Repository: findByEmail presente/ausente, existsByEmail, @PrePersist setea createdAt.
- Controller: register 200 / 400 (validación) / 409 (email duplicado), login 200 / 401.

Lo que **no** está cubierto y se añadirá:
- E2E real con Postgres (Testcontainers) → llega con CI en Paso 7.
- Test del filtro JWT (firma válida, expirado, mal formado) → pendiente, no crítico para el MVP.

## Smoke E2E con curl (verificado en local con docker compose)

```bash
docker compose up --build -d

# 1. health sigue vivo
curl http://localhost:8080/api/health
# -> {"status":"UP","service":"pixelforge-backend","timestamp":"..."}

# 2. registro de un desarrollador
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"anna@example.com","password":"secret123","displayName":"Anna","role":"DEVELOPER"}' \
  | jq -r .token)
echo "$TOKEN"

# 3. login con la misma credencial
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"anna@example.com","password":"secret123"}'

# 4. login con password mala -> 401
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"anna@example.com","password":"wrong"}'

# 5. registro con email duplicado -> 409
curl -i -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"anna@example.com","password":"another1","displayName":"X","role":"PLAYER"}'

# 6. /me sin token -> 401
curl -i http://localhost:8080/api/auth/me

# 7. /me con token -> 200 con perfil (sin passwordHash)
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"

# 8. validación: email mal formado -> 400 con detalle por campo
curl -i -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"not-email","password":"secret123","displayName":"X","role":"PLAYER"}'
```

## Cambios en infraestructura

- `pom.xml`: jjwt-api (compile) + jjwt-impl + jjwt-jackson (runtime) + h2 (test).
- `application.properties`: removido el `hibernate.dialect` hardcodeado (lo autodetecta Hibernate; era incompatible con H2 en los tests). Añadido `pixelforge.jwt.secret` y `pixelforge.jwt.expiration-ms`.
- `docker-compose.yml`: env vars `JWT_SECRET` y `JWT_EXPIRATION_MS` en el servicio backend.

## Lo que llega en el Paso 3 (frontend)

- `AuthContext` que guarda `{token, user}` en estado React + `localStorage`.
- Cliente HTTP con interceptor que adjunta `Authorization: Bearer <token>` automáticamente.
- Rutas `/login` y `/register`.
- `<ProtectedRoute role="DEVELOPER">` para gating de las páginas de desarrollador.
- Manejo del 401: limpiar token y redirigir a `/login`.
