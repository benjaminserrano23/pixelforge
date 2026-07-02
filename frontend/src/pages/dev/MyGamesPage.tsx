import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import GameCard from '../../components/GameCard'
import { deleteGame, fetchMyGames, updateGame } from '../../api/games'
import type { Game } from '../../types'

export default function MyGamesPage() {
  const [games, setGames] = useState<Game[] | 'loading' | 'error'>('loading')

  function reload() {
    setGames('loading')
    fetchMyGames()
      .then((page) => setGames(page.items))
      .catch(() => setGames('error'))
  }

  useEffect(reload, [])

  async function togglePublish(game: Game) {
    const nextStatus = game.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
    // El backend valida el DTO completo en el update, así que reenviamos los
    // campos actuales del juego con solo el status cambiado.
    await updateGame(game.id, {
      title: game.title,
      description: game.description,
      genre: game.genre,
      price: game.price,
      status: nextStatus,
    })
    reload()
  }

  async function handleDelete(game: Game) {
    if (!confirm(`¿Borrar "${game.title}"? Esta acción no se puede deshacer.`)) return
    await deleteGame(game.id)
    reload()
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Mis juegos</h1>
        <Link
          to="/dev/games/new"
          className="rounded-lg bg-indigo-600 hover:bg-indigo-500 px-3 py-1.5 text-sm font-medium transition-colors"
        >
          + Nuevo juego
        </Link>
      </div>

      {games === 'loading' && <p className="text-slate-400">Cargando…</p>}
      {games === 'error' && <p className="text-red-400">No se pudieron cargar tus juegos.</p>}

      {Array.isArray(games) && games.length === 0 && (
        <p className="text-slate-400">
          Todavía no has creado ningún juego.{' '}
          <Link to="/dev/games/new" className="text-indigo-400 hover:underline">
            Crea el primero
          </Link>
          .
        </p>
      )}

      {Array.isArray(games) && games.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {games.map((game) => (
            <div key={game.id} className="space-y-2">
              <GameCard game={game} to={`/dev/games/${game.id}/edit`} showStatus />
              <div className="flex gap-2 text-xs">
                <button
                  onClick={() => togglePublish(game)}
                  className="flex-1 rounded bg-slate-800 hover:bg-slate-700 py-1.5 transition-colors"
                >
                  {game.status === 'PUBLISHED' ? 'Despublicar' : 'Publicar'}
                </button>
                <button
                  onClick={() => handleDelete(game)}
                  className="flex-1 rounded bg-red-950 text-red-400 hover:bg-red-900 py-1.5 transition-colors"
                >
                  Borrar
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
