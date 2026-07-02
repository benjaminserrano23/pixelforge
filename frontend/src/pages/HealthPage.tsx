import { useEffect, useState } from 'react'

// Página de diagnóstico (antes era todo el App.tsx del Paso 1). Se conserva
// en /health porque el smoke test de docs/02-run-guide.md la usa para
// verificar la conexión frontend ↔ backend.

// Forma de la respuesta del backend (GET /api/health).
type Health = {
  status: string
  service: string
  timestamp: string
}

type FetchState =
  | { kind: 'loading' }
  | { kind: 'ok'; data: Health }
  | { kind: 'error'; message: string }

export default function HealthPage() {
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
    <section className="w-full max-w-md mx-auto mt-12 rounded-2xl border border-slate-800 bg-slate-900 p-8 shadow-xl">
      <h1 className="text-2xl font-bold mb-1">Diagnóstico</h1>
      <p className="text-slate-400 text-sm mb-6">
        Verificación de conexión frontend ↔ backend
      </p>

      <StatusCard state={state} />
    </section>
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
      <div className="rounded-xl bg-red-950/50 border border-red-900 p-4">
        <p className="font-semibold text-red-400 mb-1">Backend no disponible</p>
        <p className="text-sm text-red-300/80">{state.message}</p>
      </div>
    )
  }

  return (
    <div className="rounded-xl bg-emerald-950/50 border border-emerald-900 p-4">
      <p className="font-semibold text-emerald-400 mb-2">Conectado</p>
      <dl className="text-sm space-y-1">
        <div className="flex justify-between">
          <dt className="text-slate-400">Servicio</dt>
          <dd>{state.data.service}</dd>
        </div>
        <div className="flex justify-between">
          <dt className="text-slate-400">Estado</dt>
          <dd>{state.data.status}</dd>
        </div>
        <div className="flex justify-between">
          <dt className="text-slate-400">Timestamp</dt>
          <dd className="text-slate-300">{state.data.timestamp}</dd>
        </div>
      </dl>
    </div>
  )
}
