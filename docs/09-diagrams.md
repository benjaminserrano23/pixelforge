# Diagramas

Mermaid en vez de imágenes exportadas: se versiona como texto (diff legible
en cada PR) y GitHub lo renderiza nativamente en el README/docs sin build
adicional.

## Modelo de datos (ER)

```mermaid
erDiagram
    USER ||--o{ GAME : "publica (developer)"
    USER ||--o{ PURCHASE : "adquiere (player)"
    GAME ||--o{ PURCHASE : "es adquirido en"

    USER {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar display_name
        varchar role "PLAYER | DEVELOPER"
        timestamptz created_at
    }

    GAME {
        bigint id PK
        varchar title
        varchar description
        varchar genre
        numeric price
        varchar cover_image_url
        varchar status "DRAFT | PUBLISHED"
        bigint developer_id FK
        timestamptz created_at
        timestamptz updated_at
    }

    PURCHASE {
        bigint id PK
        bigint game_id FK
        bigint user_id FK
        numeric amount "snapshot del precio"
        timestamptz created_at
    }
```

`PURCHASE` tiene una restricción única `(user_id, game_id)`: un jugador no
puede comprar el mismo juego dos veces (ver `docs/08-purchases-stats.md`).
Ningún `USER` tiene ambos roles a la vez — `role` es un solo enum, no una
tabla de permisos, porque el dominio no lo necesita (spec: dos roles fijos,
sin roles compuestos).

## Backend: paquetes y responsabilidades

```mermaid
flowchart TB
    subgraph auth["auth/"]
        AuthController --> AuthService
        AuthService --> JwtService
        AuthService --> UserRepo[(UserRepository)]
    end

    subgraph game["game/"]
        GameController --> GameService
        GameService --> GameRepo[(GameRepository)]
        GameService --> CoverStorageService
        GameController --> PurchaseService
    end

    subgraph purchase["purchase/"]
        LibraryController --> PurchaseService
        StatsController --> PurchaseService
        PurchaseService --> PurchaseRepo[(PurchaseRepository)]
        PurchaseService --> GameRepo
    end

    subgraph config["config/"]
        SecurityConfig -.autoriza por rol.-> auth
        SecurityConfig -.autoriza por rol.-> game
        SecurityConfig -.autoriza por rol.-> purchase
        WebConfig -.sirve /uploads/**.-> CoverStorageService
    end
```

Cada paquete es dueño de su propio repositorio JPA; `purchase/` es el único
que depende de `game/` (necesita `GameRepository` para el precio y para las
stats), nunca al revés — evita un ciclo de dependencias entre paquetes.

## Despliegue

```mermaid
flowchart LR
    User(("Usuario")) -->|HTTPS| Vercel["Vercel\nfrontend estático (Vite build)"]
    Vercel -->|"/api/** (proxy o CORS)"| Render["Render / Railway\nbackend Spring Boot (Docker)"]
    Render --> Postgres[("Postgres administrado\nNeon / Render Postgres")]
    Render --> Volume[("Volumen de uploads\n(portadas de juegos)")]
```

Detalle de cómo configurar cada pieza en `docs/10-deploy-guide.md`.
