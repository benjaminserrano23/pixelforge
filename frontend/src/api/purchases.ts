import { api } from './client'
import type { Page, Purchase, StatsOverview } from '../types'

export function purchaseGame(gameId: number): Promise<Purchase> {
  return api<Purchase>(`/api/games/${gameId}/purchase`, { method: 'POST' })
}

export function fetchLibrary(page = 0): Promise<Page<Purchase>> {
  return api<Page<Purchase>>(`/api/library?page=${page}`)
}

export function fetchStatsOverview(): Promise<StatsOverview> {
  return api<StatsOverview>('/api/stats/overview')
}
