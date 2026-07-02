import { api, apiUpload } from './client'
import type { Game, Page } from '../types'

export type CatalogFilters = {
  genre?: string
  search?: string
  page?: number
}

function buildQuery(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') search.set(key, String(value))
  }
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}

// Catálogo público: solo juegos PUBLISHED (lo filtra el backend, no aquí).
export function fetchCatalog(filters: CatalogFilters = {}): Promise<Page<Game>> {
  return api<Page<Game>>(`/api/games${buildQuery(filters)}`)
}

// Catálogo público: 404 si el juego no existe o todavía no está PUBLISHED.
export function fetchGame(id: number): Promise<Game> {
  return api<Game>(`/api/games/${id}`)
}

export function fetchMyGames(page = 0): Promise<Page<Game>> {
  return api<Page<Game>>(`/api/games/mine${buildQuery({ page })}`)
}

// Para el formulario de edición: a diferencia de fetchGame, funciona sin
// importar el status (un juego recién creado es DRAFT, y fetchGame le daría
// 404 porque el catálogo público solo expone PUBLISHED).
export function fetchOwnedGame(id: number): Promise<Game> {
  return api<Game>(`/api/games/mine/${id}`)
}

export type GameInput = {
  title: string
  description: string
  genre: string
  price: number
  status?: 'DRAFT' | 'PUBLISHED'
}

export function createGame(input: GameInput): Promise<Game> {
  return api<Game>('/api/games', { method: 'POST', body: input })
}

export function updateGame(id: number, input: GameInput): Promise<Game> {
  return api<Game>(`/api/games/${id}`, { method: 'PUT', body: input })
}

export function deleteGame(id: number): Promise<void> {
  return api<void>(`/api/games/${id}`, { method: 'DELETE' })
}

export function uploadCover(id: number, file: File): Promise<Game> {
  const formData = new FormData()
  formData.append('file', file)
  return apiUpload<Game>(`/api/games/${id}/cover`, formData)
}
