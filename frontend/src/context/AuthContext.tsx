import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { clearToken, getToken, setToken } from '../api/client'
import type { User, UserRole } from '../types'

// Estado global de sesión. Tres estados posibles:
// - 'loading': hay un token guardado y estamos validándolo contra /api/auth/me
// - user null: sin sesión
// - user User: sesión activa
// Distinguir 'loading' evita el parpadeo de "no autenticado" al recargar la página
// antes de que /me responda.

type AuthState = {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  register: (input: authApi.RegisterInput) => Promise<User>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  // Solo arrancamos en loading si hay algo que validar.
  const [loading, setLoading] = useState(() => getToken() !== null)

  // Al montar, si hay token guardado se restaura la sesión pidiendo el perfil.
  // Si el token expiró, el cliente HTTP ya lo limpia y aquí solo apagamos loading.
  useEffect(() => {
    if (!getToken()) return
    authApi
      .me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  async function login(email: string, password: string): Promise<User> {
    const res = await authApi.login(email, password)
    setToken(res.token)
    setUser(res.user)
    return res.user
  }

  async function register(input: authApi.RegisterInput): Promise<User> {
    const res = await authApi.register(input)
    setToken(res.token)
    setUser(res.user)
    return res.user
  }

  function logout() {
    clearToken()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}

// Helper para rutas y UI condicionada por rol.
export function hasRole(user: User | null, role: UserRole): boolean {
  return user?.role === role
}
