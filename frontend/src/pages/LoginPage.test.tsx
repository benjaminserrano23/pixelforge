import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../api/auth'
import { ApiError, clearToken } from '../api/client'
import { AuthProvider } from '../context/AuthContext'
import LoginPage from './LoginPage'

vi.mock('../api/auth')

function renderLoginPage() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>Home</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearToken()
  })

  it('muestra "email o contraseña incorrectos" en un 401', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(401, { error: 'invalid_credentials' }))

    renderLoginPage()

    await user.type(screen.getByLabelText('Email'), 'anna@example.com')
    await user.type(screen.getByLabelText('Contraseña'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Email o contraseña incorrectos.')
  })

  it('navega a home tras un login exitoso', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'TOKEN',
      user: { id: 1, email: 'anna@example.com', displayName: 'Anna', role: 'PLAYER', createdAt: '' },
    })

    renderLoginPage()

    await user.type(screen.getByLabelText('Email'), 'anna@example.com')
    await user.type(screen.getByLabelText('Contraseña'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => expect(screen.getByText('Home')).toBeInTheDocument())
  })

  it('muestra un error genérico para fallas que no son 401', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockRejectedValue(new Error('network down'))

    renderLoginPage()

    await user.type(screen.getByLabelText('Email'), 'anna@example.com')
    await user.type(screen.getByLabelText('Contraseña'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo iniciar sesión. Intenta de nuevo.')
  })
})
