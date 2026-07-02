import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchGame } from '../api/games'
import type { Game } from '../types'

export default function GameDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [state, setState] = useState<{ kind: 'loading' } | { kind: 'ok'; game: Game } | { kind: 'not_found' } | { kind: 'error' }>({
    kind: 'loading',
  })

  useEffect(() => {
    setState({ kind: 'loading' })
    fetchGame(Number(id))
      .then((game) => setState({ kind: 'ok', game }))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 404) setState({ kind: 'not_found' })
        else setState({ kind: 'error' })
      })
  }, [id])

  if (state.kind === 'loading') {
    return <p className="text-slate-400 text-center mt-12">Cargando…</p>
  }

  if (state.kind === 'not_found') {
    return (
      <div className="text-center mt-12">
        <p className="text-slate-300">Este juego no existe o todavía no está publicado.</p>
        <Link to="/" className="text-indigo-400 hover:underline text-sm">
          Volver al catálogo
        </Link>
      </div>
    )
  }

  if (state.kind === 'error') {
    return <p className="text-red-400 text-center mt-12">No se pudo cargar el juego.</p>
  }

  const { game } = state

  return (
    <div className="max-w-3xl mx-auto">
      <Link to="/" className="text-sm text-slate-400 hover:text-white transition-colors">
        ← Catálogo
      </Link>

      <div className="mt-4 grid sm:grid-cols-2 gap-6">
        <div className="aspect-video bg-slate-900 border border-slate-800 rounded-xl flex items-center justify-center overflow-hidden">
          {game.coverImageUrl ? (
            <img src={game.coverImageUrl} alt={game.title} className="w-full h-full object-cover" />
          ) : (
            <span className="text-slate-600 text-sm">Sin portada</span>
          )}
        </div>

        <div>
          <p className="text-xs text-slate-500 uppercase tracking-wide">{game.genre}</p>
          <h1 className="text-2xl font-bold mt-1">{game.title}</h1>
          <p className="text-xl text-indigo-400 mt-2">${game.price.toFixed(2)}</p>
          <p className="text-slate-300 mt-4 whitespace-pre-line">{game.description}</p>

          {/* Adquirir un juego (biblioteca) llega en el Paso 6 del roadmap. */}
          <button
            disabled
            title="Disponible en el Paso 6 del roadmap"
            className="mt-6 w-full rounded-lg bg-slate-800 text-slate-500 py-2 text-sm font-medium cursor-not-allowed"
          >
            Adquirir (próximamente)
          </button>
        </div>
      </div>
    </div>
  )
}
