import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { UserRole } from '../types'

// Guarda de rutas. Se usa como elemento padre en el router:
//   <Route element={<ProtectedRoute role="DEVELOPER" />}> ...rutas hijas... </Route>
// Sin sesión redirige a /login (recordando de dónde venía para volver tras loguearse);
// con sesión pero rol equivocado redirige a la home en vez de mostrar un 403 hostil.

export default function ProtectedRoute({ role }: { role?: UserRole }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center text-slate-400">
        Cargando sesión…
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (role && user.role !== role) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
