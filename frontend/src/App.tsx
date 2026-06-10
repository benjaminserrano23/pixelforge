import { useEffect, useState } from 'react'

// Forma de la respuesta del backend (GET /api/health).
// Tiparlo aquí da autocompletado en el resto del componente y
// hace explícito el contrato con el backend.
type Health = {
  status: string
  service: string
  timestamp: string
}

type FetchState =
  | { kind: 'loading' }
  | { kind: 'ok'; data: Health }
  | { kind: 'error'; message: string }

export default function App() {
  const [state, setState] = useState<FetchState>({ kind: 'loading' })

  useEffect(() => {
    const controller = new AbortController()

    // Path relativo: en dev lo intercepta el proxy de Vite; en producción
    // lo proxy-pasa nginx al backend. El componente no sabe (ni debe saber)
    // dónde vive el backend.
    fetch('/api/health', { signal: controller.signal })
      .then(async (res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        const data: Health = await res.json()
        setState({ kind: 'ok', data })
      })
      .catch((err: unknown) => {
        if (err instanceof DOMException && err.name === 'AbortError') return
        const message = err instanceof Error ? err.message : 'Error desconocido'
        setState({ kind: 'error', message })
      })

    return () => controller.abort()
  }, [])

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-6">
      <section className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-900 p-8 shadow-xl">
        <h1 className="text-2xl font-bold mb-1">pixelforge</h1>
        <p className="text-slate-400 text-sm mb-6">
          Verificación de conexión frontend ↔ backend
        </p>

        <StatusCard state={state} />
      </section>
    </main>
  )
}

function StatusCard({ state }: { state: FetchState }) {
  if (state.kind === 'loading') {
    return (
      <div className="rounded-xl bg-slate-800 p-4 text-slate-300">
        Consultando <code className="text-slate-100">/api/health</code>…
      </div>
    )
  }

  if (state.kind === 'error') {
    return (
      <div className="rounded-xl bg-red-950/60 border border-red-800 p-4">
        <p className="text-red-300 font-semibold">Backend no responde</p>
        <p className="text-red-400/80 text-sm mt-1">{state.message}</p>
      </div>
    )
  }

  const { data } = state
  return (
    <div className="rounded-xl bg-emerald-950/60 border border-emerald-800 p-4">
      <p className="text-emerald-300 font-semibold">
        Estado: {data.status}
      </p>
      <dl className="mt-3 text-sm grid grid-cols-[6rem_1fr] gap-y-1 text-emerald-200/90">
        <dt className="text-emerald-400/70">Servicio</dt>
        <dd>{data.service}</dd>
        <dt className="text-emerald-400/70">Timestamp</dt>
        <dd className="font-mono text-xs">{data.timestamp}</dd>
      </dl>
    </div>
  )
}
