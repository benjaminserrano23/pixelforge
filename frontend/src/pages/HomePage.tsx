import { useEffect, useState } from 'react'
import GameCard from '../components/GameCard'
import { fetchCatalog } from '../api/games'
import type { Game, Page } from '../types'

// Catálogo público. Sin debounce en la búsqueda: el volumen esperado (un
// portafolio, no un e-commerce real) no justifica la complejidad extra;
// cada tecleo dispara un fetch, pero /api/games es una query liviana.
export default function HomePage() {
  const [genre, setGenre] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<Page<Game> | 'loading' | 'error'>('loading')

  useEffect(() => {
    setResult('loading')
    const controller = new AbortController()
    fetchCatalog({ genre: genre || undefined, search: search || undefined, page })
      .then((data) => {
        if (!controller.signal.aborted) setResult(data)
      })
      .catch(() => {
        if (!controller.signal.aborted) setResult('error')
      })
    return () => controller.abort()
  }, [genre, search, page])

  function resetToFirstPage<T>(setter: (v: T) => void) {
    return (value: T) => {
      setPage(0)
      setter(value)
    }
  }

  return (
    <div>
      <div className="flex flex-col sm:flex-row gap-3 mb-8">
        <input
          type="search"
          placeholder="Buscar por título…"
          value={search}
          onChange={(e) => resetToFirstPage(setSearch)(e.target.value)}
          className="flex-1 rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-sm outline-none focus:border-indigo-500"
        />
        <input
          type="text"
          placeholder="Género (ej. Platformer)"
          value={genre}
          onChange={(e) => resetToFirstPage(setGenre)(e.target.value)}
          className="sm:w-56 rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-sm outline-none focus:border-indigo-500"
        />
      </div>

      {result === 'loading' && <p className="text-slate-400 text-center mt-12">Cargando catálogo…</p>}

      {result === 'error' && (
        <p className="text-red-400 text-center mt-12">No se pudo cargar el catálogo. Intenta de nuevo.</p>
      )}

      {typeof result === 'object' && result.items.length === 0 && (
        <p className="text-slate-400 text-center mt-12">
          {search || genre ? 'Ningún juego coincide con el filtro.' : 'Todavía no hay juegos publicados.'}
        </p>
      )}

      {typeof result === 'object' && result.items.length > 0 && (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {result.items.map((game) => (
              <GameCard key={game.id} game={game} to={`/games/${game.id}`} />
            ))}
          </div>

          {result.totalPages > 1 && (
            <div className="flex items-center justify-center gap-4 mt-8 text-sm text-slate-400">
              <button
                disabled={result.page === 0}
                onClick={() => setPage((p) => p - 1)}
                className="disabled:opacity-30 hover:text-white transition-colors"
              >
                ← Anterior
              </button>
              <span>
                Página {result.page + 1} de {result.totalPages}
              </span>
              <button
                disabled={result.page >= result.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="disabled:opacity-30 hover:text-white transition-colors"
              >
                Siguiente →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
