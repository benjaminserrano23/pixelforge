import { useEffect, useState } from 'react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { fetchStatsOverview } from '../../api/purchases'
import type { StatsOverview } from '../../types'

export default function StatsPage() {
  const [stats, setStats] = useState<StatsOverview | 'loading' | 'error'>('loading')

  useEffect(() => {
    fetchStatsOverview()
      .then(setStats)
      .catch(() => setStats('error'))
  }, [])

  if (stats === 'loading') return <p className="text-slate-400 text-center mt-12">Cargando…</p>
  if (stats === 'error') return <p className="text-red-400 text-center mt-12">No se pudieron cargar las estadísticas.</p>

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Estadísticas</h1>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
        <StatCard label="Juegos" value={stats.totalGames} />
        <StatCard label="Publicados" value={stats.publishedGames} />
        <StatCard label="Adquisiciones" value={stats.totalPurchases} />
        <StatCard label="Ingresos" value={`$${stats.totalRevenue.toFixed(2)}`} />
      </div>

      {stats.perGame.length === 0 ? (
        <p className="text-slate-400">Todavía no tienes juegos. Crea uno para ver sus estadísticas aquí.</p>
      ) : (
        <div className="rounded-xl border border-slate-800 bg-slate-900 p-4">
          <h2 className="text-sm font-medium text-slate-300 mb-4">Adquisiciones por juego</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={stats.perGame}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
              <XAxis dataKey="title" tick={{ fill: '#94a3b8', fontSize: 12 }} />
              <YAxis allowDecimals={false} tick={{ fill: '#94a3b8', fontSize: 12 }} />
              <Tooltip
                contentStyle={{ background: '#0f172a', border: '1px solid #1e293b', borderRadius: 8 }}
                labelStyle={{ color: '#e2e8f0' }}
              />
              <Bar dataKey="purchases" name="Adquisiciones" fill="#6366f1" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900 p-4">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="text-2xl font-bold mt-1">{value}</p>
    </div>
  )
}
