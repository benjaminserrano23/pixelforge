import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// Home provisional: el catálogo real llega en el Paso 5 (cuando exista la
// entidad Game en el backend). Mientras tanto la página demuestra el estado
// de sesión y sirve de punto de aterrizaje para las rutas protegidas.

export default function HomePage() {
  const { user } = useAuth()

  return (
    <div className="mt-8 text-center">
      <h1 className="text-3xl font-bold mb-2">Catálogo</h1>
      <p className="text-slate-400 mb-8">
        Aquí vivirá el catálogo público de juegos (Paso 5 del roadmap).
      </p>

      <div className="max-w-md mx-auto rounded-2xl border border-slate-800 bg-slate-900 p-6 text-left">
        {user ? (
          <p className="text-sm text-slate-300">
            Sesión activa como <span className="font-medium text-white">{user.displayName}</span>{' '}
            ({user.role === 'DEVELOPER' ? 'Desarrollador' : 'Jugador'}).{' '}
            {user.role === 'DEVELOPER' ? (
              <Link to="/dev/games" className="text-indigo-400 hover:underline">
                Ir a mis juegos →
              </Link>
            ) : (
              <Link to="/library" className="text-indigo-400 hover:underline">
                Ir a mi biblioteca →
              </Link>
            )}
          </p>
        ) : (
          <p className="text-sm text-slate-300">
            No has iniciado sesión.{' '}
            <Link to="/login" className="text-indigo-400 hover:underline">
              Entra
            </Link>{' '}
            o{' '}
            <Link to="/register" className="text-indigo-400 hover:underline">
              crea una cuenta
            </Link>{' '}
            para probar las rutas protegidas por rol.
          </p>
        )}
      </div>
    </div>
  )
}
