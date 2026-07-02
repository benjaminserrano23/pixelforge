import { act, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../api/auth'
import { clearToken, getToken, setToken } from '../api/client'
import { AuthProvider, useAuth } from './AuthContext'
import type { User } from '../types'

vi.mock('../api/auth')

const sampleUser: User = {
  id: 1,
  email: 'anna@example.com',
  displayName: 'Anna',
  role: 'PLAYER',
  createdAt: '2026-07-02T00:00:00Z',
}

// Componente de prueba: expone el estado del contexto como texto para que
// los tests puedan leerlo con getByText/queryByText.
function Probe() {
  const { user, loading, login, logout } = useAuth()
  return (
    <div>
      <span data-testid="loading">{String(loading)}</span>
      <span data-testid="user">{user ? user.displayName : 'none'}</span>
      <button onClick={() => login('anna@example.com', 'secret123')}>login</button>
      <button onClick={logout}>logout</button>
    </div>
  )
}

describe('AuthProvider', () => {
  beforeEach(() => {
    clearToken()
    vi.clearAllMocks()
  })

  it('arranca sin loading cuando no hay token guardado', () => {
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    expect(screen.getByTestId('loading')).toHaveTextContent('false')
    expect(screen.getByTestId('user')).toHaveTextContent('none')
  })

  it('restaura la sesión llamando a /me cuando hay un token guardado', async () => {
    setToken('EXISTING_TOKEN')
    vi.mocked(authApi.me).mockResolvedValue(sampleUser)

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    // Antes de que /me resuelva, loading debe ser true (evita el parpadeo de "sin sesión").
    expect(screen.getByTestId('loading')).toHaveTextContent('true')

    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('Anna'))
    expect(screen.getByTestId('loading')).toHaveTextContent('false')
  })

  it('limpia la sesión si /me falla (token inválido o expirado)', async () => {
    setToken('EXPIRED_TOKEN')
    vi.mocked(authApi.me).mockRejectedValue(new Error('401'))

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))
    expect(screen.getByTestId('user')).toHaveTextContent('none')
  })

  it('login guarda el token y actualiza el usuario', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'NEW_TOKEN', user: sampleUser })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await act(async () => {
      screen.getByText('login').click()
    })

    expect(screen.getByTestId('user')).toHaveTextContent('Anna')
    expect(getToken()).toBe('NEW_TOKEN')
  })

  it('logout limpia el token y el usuario', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'NEW_TOKEN', user: sampleUser })

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await act(async () => {
      screen.getByText('login').click()
    })
    act(() => {
      screen.getByText('logout').click()
    })

    expect(screen.getByTestId('user')).toHaveTextContent('none')
    expect(getToken()).toBeNull()
  })
})
