import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../api/auth'
import { ApiError, clearToken } from '../api/client'
import { AuthProvider } from '../context/AuthContext'
import RegisterPage from './RegisterPage'

vi.mock('../api/auth')

function renderRegisterPage() {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <AuthProvider>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/" element={<div>Home</div>} />
          <Route path="/dev/games" element={<div>Mis juegos</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Nombre visible'), 'Anna')
  await user.type(screen.getByLabelText('Email'), 'anna@example.com')
  await user.type(screen.getByLabelText('Contraseña (mínimo 8 caracteres)'), 'secret123')
  await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearToken()
  })

  it('muestra el error de email duplicado en un 409', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.register).mockRejectedValue(new ApiError(409, { error: 'email_already_used' }))

    renderRegisterPage()
    await fillAndSubmit(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('Ese email ya está registrado.')
  })

  it('mapea los errores de campo del backend (400 validation_failed) bajo cada input', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.register).mockRejectedValue(
      new ApiError(400, { error: 'validation_failed', fields: { email: 'must be a well-formed email address' } }),
    )

    renderRegisterPage()
    await fillAndSubmit(user)

    expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument()
  })

  it('un DEVELOPER recién registrado aterriza en /dev/games', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.register).mockResolvedValue({
      token: 'TOKEN',
      user: { id: 1, email: 'dev@example.com', displayName: 'Dev', role: 'DEVELOPER', createdAt: '' },
    })

    renderRegisterPage()
    await user.click(screen.getByRole('button', { name: /Desarrollador/ }))
    await fillAndSubmit(user)

    await waitFor(() => expect(screen.getByText('Mis juegos')).toBeInTheDocument())
  })

  it('un PLAYER recién registrado aterriza en la home', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.register).mockResolvedValue({
      token: 'TOKEN',
      user: { id: 1, email: 'player@example.com', displayName: 'Player', role: 'PLAYER', createdAt: '' },
    })

    renderRegisterPage()
    await fillAndSubmit(user)

    await waitFor(() => expect(screen.getByText('Home')).toBeInTheDocument())
  })
})
