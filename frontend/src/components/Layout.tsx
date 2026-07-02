import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// Shell común: navbar arriba, contenido de la ruta activa abajo.
// La navbar cambia según la sesión: links de rol (biblioteca para PLAYER,
// panel para DEVELOPER) y login/logout.

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-3 py-1.5 rounded-lg text-sm transition-colors ${
      isActive ? 'bg-slate-800 text-white' : 'text-slate-400 hover:text-white'
    }`

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800">
        <nav className="mx-auto max-w-5xl px-4 h-14 flex items-center gap-2">
          <Link to="/" className="font-bold text-lg mr-4">
            pixel<span className="text-indigo-400">forge</span>
          </Link>

          <NavLink to="/" end className={linkClass}>
            Catálogo
          </NavLink>
          {user?.role === 'PLAYER' && (
            <NavLink to="/library" className={linkClass}>
              Mi biblioteca
            </NavLink>
          )}
          {user?.role === 'DEVELOPER' && (
            <>
              <NavLink to="/dev/games" className={linkClass}>
                Mis juegos
              </NavLink>
              <NavLink to="/dev/stats" className={linkClass}>
                Estadísticas
              </NavLink>
            </>
          )}

          <div className="ml-auto flex items-center gap-3">
            {user ? (
              <>
                <span className="text-sm text-slate-400">
                  {user.displayName}
                  <span className="ml-1.5 text-xs rounded bg-slate-800 px-1.5 py-0.5 text-slate-300">
                    {user.role === 'DEVELOPER' ? 'Desarrollador' : 'Jugador'}
                  </span>
                </span>
                <button
                  onClick={handleLogout}
                  className="text-sm text-slate-400 hover:text-white transition-colors"
                >
                  Salir
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login" className={linkClass}>
                  Entrar
                </NavLink>
                <NavLink
                  to="/register"
                  className="px-3 py-1.5 rounded-lg text-sm bg-indigo-600 hover:bg-indigo-500 text-white transition-colors"
                >
                  Crear cuenta
                </NavLink>
              </>
            )}
          </div>
        </nav>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
