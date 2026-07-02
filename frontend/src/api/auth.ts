import { api } from './client'
import type { AuthResponse, User, UserRole } from '../types'

// Funciones de la API de auth. Una función por endpoint mantiene los
// componentes libres de detalles HTTP: ellos llaman "register", no "POST /api/auth/register".

export type RegisterInput = {
  email: string
  password: string
  displayName: string
  role: UserRole
}

export function register(input: RegisterInput): Promise<AuthResponse> {
  return api<AuthResponse>('/api/auth/register', { method: 'POST', body: input })
}

export function login(email: string, password: string): Promise<AuthResponse> {
  return api<AuthResponse>('/api/auth/login', { method: 'POST', body: { email, password } })
}

export function me(): Promise<User> {
  return api<User>('/api/auth/me')
}
