import { Link } from 'react-router-dom'
import type { Game } from '../types'

// Tarjeta usada tanto en el catálogo público como en "Mis juegos". La única
// diferencia entre esos dos contextos es a dónde apunta el link y si se
// muestra el status — ambas cosas las decide quien la usa, no la tarjeta.
export default function GameCard({ game, to, showStatus = false }: {
  game: Game
  to: string
  showStatus?: boolean
}) {
  return (
    <Link
      to={to}
      className="block rounded-xl border border-slate-800 bg-slate-900 overflow-hidden hover:border-slate-600 transition-colors"
    >
      <div className="aspect-video bg-slate-800 flex items-center justify-center overflow-hidden">
        {game.coverImageUrl ? (
          <img src={game.coverImageUrl} alt={game.title} className="w-full h-full object-cover" />
        ) : (
          <span className="text-slate-600 text-sm">Sin portada</span>
        )}
      </div>
      <div className="p-4">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-semibold">{game.title}</h3>
          {showStatus && (
            <span
              className={`shrink-0 text-xs rounded px-1.5 py-0.5 ${
                game.status === 'PUBLISHED'
                  ? 'bg-emerald-950 text-emerald-400 border border-emerald-900'
                  : 'bg-slate-800 text-slate-400 border border-slate-700'
              }`}
            >
              {game.status === 'PUBLISHED' ? 'Publicado' : 'Borrador'}
            </span>
          )}
        </div>
        <p className="text-xs text-slate-500 mt-1">{game.genre}</p>
        <p className="text-sm text-slate-300 mt-2">${game.price.toFixed(2)}</p>
      </div>
    </Link>
  )
}
