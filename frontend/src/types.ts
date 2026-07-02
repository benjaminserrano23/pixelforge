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
// { error: "validation_failed", fields: {campo: mensaje} } o { error: "invalid_credentials" }.
export type ApiErrorBody = {
  error: string
  fields?: Record<string, string>
}
