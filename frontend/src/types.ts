// Tipos compartidos que reflejan los DTOs del backend.
// Mantenerlos en un solo archivo hace explícito el contrato con la API:
// si el backend cambia un record, este es el único lugar que hay que tocar.

export type UserRole = 'PLAYER' | 'DEVELOPER'

export type User = {
  id: number
  email: string
  displayName: string
  role: UserRole
  createdAt: string
}

// Respuesta de POST /api/auth/register y /api/auth/login.
export type AuthResponse = {
  token: string
  user: User
}

// Formato de error del GlobalExceptionHandler del backend:
// { error: "validation_failed", fields: {...} }, { error: "invalid_cover", message: "..." },
// o simplemente { error: "invalid_credentials" }.
export type ApiErrorBody = {
  error: string
  fields?: Record<string, string>
  message?: string
}

export type GameStatus = 'DRAFT' | 'PUBLISHED'

export type Game = {
  id: number
  title: string
  description: string
  genre: string
  price: number
  coverImageUrl: string | null
  status: GameStatus
  developerId: number
  createdAt: string
  updatedAt: string
}

// Espejo de PageResponse<T> del backend (GameController: catalog, mine).
export type Page<T> = {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

// Respuesta de POST /api/games/{id}/purchase y de cada item de GET /api/library.
export type Purchase = {
  id: number
  game: Game
  amount: number
  createdAt: string
}

// Una fila del gráfico de stats (un juego del desarrollador).
export type GameStatsEntry = {
  gameId: number
  title: string
  purchases: number
  revenue: number
}

// Respuesta de GET /api/stats/overview.
export type StatsOverview = {
  totalGames: number
  publishedGames: number
  totalPurchases: number
  totalRevenue: number
  perGame: GameStatsEntry[]
}
