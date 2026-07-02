import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import ProtectedRoute from './ProtectedRoute'
import * as AuthContextModule from '../context/AuthContext'
import type { User } from '../types'

vi.mock('../context/AuthContext', async () => {
  const actual = await vi.importActual<typeof AuthContextModule>('../context/AuthContext')
  return { ...actual, useAuth: vi.fn() }
})

const useAuthMock = vi.mocked(AuthContextModule.useAuth)

function renderProtected(role?: 'PLAYER' | 'DEVELOPER') {
  return render(
    <MemoryRouter initialEntries={['/dev/games']}>
      <Routes>
        <Route element={<ProtectedRoute role={role} />}>
          <Route path="/dev/games" element={<div>Zona protegida</div>} />
        </Route>
        <Route path="/login" element={<div>Página de login</div>} />
        <Route path="/" element={<div>Home</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

const player: User = { id: 1, email: 'p@example.com', displayName: 'P', role: 'PLAYER', createdAt: '' }
const developer: User = { id: 2, email: 'd@example.com', displayName: 'D', role: 'DEVELOPER', createdAt: '' }

describe('ProtectedRoute', () => {
  it('muestra el indicador de carga mientras se restaura la sesión', () => {
    useAuthMock.mockReturnValue({ user: null, loading: true, login: vi.fn(), register: vi.fn(), logout: vi.fn() })

    renderProtected()

    expect(screen.getByText('Cargando sesión…')).toBeInTheDocument()
  })

  it('redirige a /login cuando no hay sesión', () => {
    useAuthMock.mockReturnValue({ user: null, loading: false, login: vi.fn(), register: vi.fn(), logout: vi.fn() })

    renderProtected()

    expect(screen.getByText('Página de login')).toBeInTheDocument()
  })

  it('redirige a home cuando el rol no coincide', () => {
    useAuthMock.mockReturnValue({ user: player, loading: false, login: vi.fn(), register: vi.fn(), logout: vi.fn() })

    renderProtected('DEVELOPER')

    expect(screen.getByText('Home')).toBeInTheDocument()
  })

  it('renderiza la ruta hija cuando el rol coincide', () => {
    useAuthMock.mockReturnValue({ user: developer, loading: false, login: vi.fn(), register: vi.fn(), logout: vi.fn() })

    renderProtected('DEVELOPER')

    expect(screen.getByText('Zona protegida')).toBeInTheDocument()
  })

  it('renderiza la ruta hija sin restricción de rol si hay cualquier sesión', () => {
    useAuthMock.mockReturnValue({ user: player, loading: false, login: vi.fn(), register: vi.fn(), logout: vi.fn() })

    renderProtected(undefined)

    expect(screen.getByText('Zona protegida')).toBeInTheDocument()
  })
})
