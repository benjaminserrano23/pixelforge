import type { ApiErrorBody } from '../types'

// Cliente HTTP mínimo sobre fetch. Centraliza tres responsabilidades que no
// queremos repetir en cada llamada: adjuntar el JWT, serializar JSON y
// convertir respuestas no-2xx en errores tipados que la UI pueda mostrar.

const TOKEN_KEY = 'pixelforge_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

// Error de API con el cuerpo del GlobalExceptionHandler del backend.
// Extiende Error para que un catch genérico también lo pueda tratar.
export class ApiError extends Error {
  status: number
  body: ApiErrorBody | null

  constructor(status: number, body: ApiErrorBody | null) {
    super(body?.error ?? `HTTP ${status}`)
    this.status = status
    this.body = body
  }
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {}

  // El interceptor: toda petición sale con el token si existe.
  // El backend ignora el header en endpoints públicos, así que no hace
  // falta distinguir aquí entre rutas públicas y protegidas.
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'

  const res = await fetch(path, {
    method: options.method ?? 'GET',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })

  if (!res.ok) {
    // 401 con token guardado = token expirado o inválido: lo limpiamos para
    // que el AuthContext no siga intentando restaurar una sesión muerta.
    if (res.status === 401 && token) clearToken()

    let body: ApiErrorBody | null = null
    try {
      body = await res.json()
    } catch {
      // Respuesta sin cuerpo JSON (p. ej. 500 del proxy): se conserva solo el status.
    }
    throw new ApiError(res.status, body)
  }

  // 204 u otras respuestas sin cuerpo.
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}
