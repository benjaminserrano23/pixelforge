import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import HealthPage from './pages/HealthPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import PlaceholderPage from './pages/PlaceholderPage'
import RegisterPage from './pages/RegisterPage'
import './index.css'

// Árbol de rutas del spec (sección 4). Las zonas /dev/* y /library existen
// desde ya como placeholders para poder probar la protección por rol;
// su contenido real llega en los Pasos 5 y 6.

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route path="health" element={<HealthPage />} />

            <Route element={<ProtectedRoute role="PLAYER" />}>
              <Route path="library" element={<PlaceholderPage title="Mi biblioteca" step={6} />} />
            </Route>

            <Route element={<ProtectedRoute role="DEVELOPER" />}>
              <Route path="dev/games" element={<PlaceholderPage title="Mis juegos" step={5} />} />
              <Route path="dev/stats" element={<PlaceholderPage title="Estadísticas" step={6} />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </StrictMode>,
)
