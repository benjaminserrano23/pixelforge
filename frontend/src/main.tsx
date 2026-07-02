import { lazy, StrictMode, Suspense } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import GameDetailPage from './pages/GameDetailPage'
import GameFormPage from './pages/dev/GameFormPage'
import HealthPage from './pages/HealthPage'
import HomePage from './pages/HomePage'
import LibraryPage from './pages/LibraryPage'
import LoginPage from './pages/LoginPage'
import MyGamesPage from './pages/dev/MyGamesPage'
import RegisterPage from './pages/RegisterPage'
import './index.css'

// Recharts es ~150KB por sí solo y solo lo usa esta página: cargarlo lazy
// evita que todo el mundo (incluido el catálogo público, sin sesión) pague
// ese peso en el bundle inicial.
const StatsPage = lazy(() => import('./pages/dev/StatsPage'))

// Árbol de rutas del spec (sección 4), completo desde el Paso 6: catálogo,
// juegos, biblioteca y estadísticas consumen todos la API real.

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="games/:id" element={<GameDetailPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route path="health" element={<HealthPage />} />

            <Route element={<ProtectedRoute role="PLAYER" />}>
              <Route path="library" element={<LibraryPage />} />
            </Route>

            <Route element={<ProtectedRoute role="DEVELOPER" />}>
              <Route path="dev/games" element={<MyGamesPage />} />
              <Route path="dev/games/new" element={<GameFormPage />} />
              <Route path="dev/games/:id/edit" element={<GameFormPage />} />
              <Route
                path="dev/stats"
                element={
                  <Suspense fallback={<p className="text-slate-400 text-center mt-12">Cargando…</p>}>
                    <StatsPage />
                  </Suspense>
                }
              />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </StrictMode>,
)
