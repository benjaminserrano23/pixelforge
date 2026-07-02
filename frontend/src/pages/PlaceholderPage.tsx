// Página genérica para rutas cuyo contenido llega en pasos posteriores del
// roadmap (biblioteca, mis juegos, estadísticas). Existir ya como rutas
// protegidas permite verificar la lógica de roles antes de construir la UI real.

export default function PlaceholderPage({ title, step }: { title: string; step: number }) {
  return (
    <div className="mt-8 text-center">
      <h1 className="text-3xl font-bold mb-2">{title}</h1>
      <p className="text-slate-400">
        En construcción — llega en el Paso {step} del roadmap.
      </p>
    </div>
  )
}
