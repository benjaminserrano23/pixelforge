import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import GameDetailPage from './pages/GameDetailPage'
import GameFormPage from './pages/dev/GameFormPage'
import HealthPage from './pages/HealthPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import MyGamesPage from './pages/dev/MyGamesPage'
import PlaceholderPage from './pages/PlaceholderPage'
import RegisterPage from './pages/RegisterPage'
import './index.css'

// Árbol de rutas del spec (sección 4). /library y /dev/stats siguen como
// placeholders (llegan en el Paso 6); el catálogo y la zona de desarrollador
// ya consumen la API real de juegos del Paso 4.

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
              <Route path="library" element={<PlaceholderPage title="Mi biblioteca" step={6} />} />
            </Route>

            <Route element={<ProtectedRoute role="DEVELOPER" />}>
              <Route path="dev/games" element={<MyGamesPage />} />
              <Route path="dev/games/new" element={<GameFormPage />} />
              <Route path="dev/games/:id/edit" element={<GameFormPage />} />
              <Route path="dev/stats" element={<PlaceholderPage title="Estadísticas" step={6} />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </StrictMode>,
)
