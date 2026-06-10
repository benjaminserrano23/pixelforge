import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vitejs.dev/config/
export default defineConfig({
  // Plugins:
  //  - react(): Fast Refresh + JSX transform
  //  - tailwindcss(): integración oficial v4, reemplaza a PostCSS + tailwind.config.js
  plugins: [react(), tailwindcss()],

  server: {
    host: true,        // escuchar en 0.0.0.0 para que funcione dentro de Docker
    port: 5173,
    // En dev, el navegador llama a /api/health y Vite lo proxy-pasa al backend.
    // Así el código de React nunca conoce el host del backend → mismo build vale para prod.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
