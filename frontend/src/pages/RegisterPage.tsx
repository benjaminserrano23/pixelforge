import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import FormField from '../components/FormField'
import { useAuth } from '../context/AuthContext'
import type { UserRole } from '../types'

// Mapea los mensajes de validación del backend (en inglés, generados por
// Bean Validation) a los campos del formulario. Se muestran tal cual bajo
// cada campo; el error general (email duplicado) va arriba del botón.

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [role, setRole] = useState<UserRole>('PLAYER')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)
    try {
      const user = await register({ email, password, displayName, role })
      // El registro deja la sesión iniciada; cada rol aterriza en su zona.
      navigate(user.role === 'DEVELOPER' ? '/dev/games' : '/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError('Ese email ya está registrado.')
      } else if (err instanceof ApiError && err.status === 400 && err.body?.fields) {
        setFieldErrors(err.body.fields)
      } else {
        setError('No se pudo crear la cuenta. Intenta de nuevo.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  const roleOption = (value: UserRole, title: string, subtitle: string) => (
    <button
      type="button"
      onClick={() => setRole(value)}
      className={`flex-1 rounded-lg border px-3 py-2 text-left transition-colors ${
        role === value
          ? 'border-indigo-500 bg-indigo-950/40'
          : 'border-slate-700 bg-slate-800 hover:border-slate-500'
      }`}
    >
      <span className="block text-sm font-medium">{title}</span>
      <span className="block text-xs text-slate-400">{subtitle}</span>
    </button>
  )

  return (
    <div className="max-w-sm mx-auto mt-12">
      <h1 className="text-2xl font-bold mb-1">Crear cuenta</h1>
      <p className="text-sm text-slate-400 mb-6">
        ¿Ya tienes una?{' '}
        <Link to="/login" className="text-indigo-400 hover:underline">
          Inicia sesión
        </Link>
      </p>

      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <FormField
          id="displayName"
          label="Nombre visible"
          autoComplete="nickname"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          error={fieldErrors.displayName}
          required
        />
        <FormField
          id="email"
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={fieldErrors.email}
          required
        />
        <FormField
          id="password"
          label="Contraseña (mínimo 8 caracteres)"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          required
        />

        <div>
          <span className="block text-sm text-slate-300 mb-1">Quiero usar PixelForge como</span>
          <div className="flex gap-2">
            {roleOption('PLAYER', 'Jugador', 'Explorar y adquirir juegos')}
            {roleOption('DEVELOPER', 'Desarrollador', 'Publicar mis juegos')}
          </div>
        </div>

        {error && (
          <p role="alert" className="text-sm text-red-400 rounded-lg bg-red-950/40 border border-red-900 px-3 py-2">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-lg bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 py-2 text-sm font-medium transition-colors"
        >
          {submitting ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
      </form>
    </div>
  )
}
