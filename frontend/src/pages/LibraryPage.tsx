import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import GameCard from '../components/GameCard'
import { fetchLibrary } from '../api/purchases'
import type { Purchase } from '../types'

export default function LibraryPage() {
  const [purchases, setPurchases] = useState<Purchase[] | 'loading' | 'error'>('loading')

  useEffect(() => {
    fetchLibrary()
      .then((page) => setPurchases(page.items))
      .catch(() => setPurchases('error'))
  }, [])

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Mi biblioteca</h1>

      {purchases === 'loading' && <p className="text-slate-400">Cargando…</p>}
      {purchases === 'error' && <p className="text-red-400">No se pudo cargar tu biblioteca.</p>}

      {Array.isArray(purchases) && purchases.length === 0 && (
        <p className="text-slate-400">
          Todavía no has adquirido ningún juego.{' '}
          <Link to="/" className="text-indigo-400 hover:underline">
            Explora el catálogo
          </Link>
          .
        </p>
      )}

      {Array.isArray(purchases) && purchases.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {purchases.map((purchase) => (
            <GameCard key={purchase.id} game={purchase.game} to={`/games/${purchase.game.id}`} />
          ))}
        </div>
      )}
    </div>
  )
}
