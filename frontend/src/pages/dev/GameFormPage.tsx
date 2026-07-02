import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import FormField from '../../components/FormField'
import { ApiError } from '../../api/client'
import { createGame, fetchOwnedGame, updateGame, uploadCover } from '../../api/games'
import type { Game } from '../../types'

// Un solo formulario para crear y editar: la única diferencia es si "id"
// viene en la URL. La portada solo puede subirse en modo edición porque el
// backend la asocia a un juego que ya existe (POST /api/games/{id}/cover).
export default function GameFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = id !== undefined
  const navigate = useNavigate()

  const [game, setGame] = useState<Game | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [genre, setGenre] = useState('')
  const [price, setPrice] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [coverUploading, setCoverUploading] = useState(false)
  const [loading, setLoading] = useState(isEdit)

  useEffect(() => {
    if (!isEdit) return
    fetchOwnedGame(Number(id))
      .then((g) => {
        setGame(g)
        setTitle(g.title)
        setDescription(g.description)
        setGenre(g.genre)
        setPrice(String(g.price))
      })
      .catch(() => setError('No se pudo cargar el juego.'))
      .finally(() => setLoading(false))
  }, [id, isEdit])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)
    try {
      const input = { title, description, genre, price: Number(price) }
      if (isEdit) {
        // Conservamos el status actual: este formulario no decide publicar/
        // despublicar, eso lo hace el botón dedicado en "Mis juegos".
        await updateGame(Number(id), { ...input, status: game?.status })
        navigate('/dev/games')
      } else {
        // Vamos directo a editar el juego recién creado: recién ahí existe un
        // id contra el que se puede subir la portada.
        const created = await createGame(input)
        navigate(`/dev/games/${created.id}/edit`)
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 400 && err.body?.fields) {
        setFieldErrors(err.body.fields)
      } else {
        setError('No se pudo guardar el juego. Revisa los datos e intenta de nuevo.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCoverChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file || !game) return
    setCoverUploading(true)
    setError(null)
    try {
      const updated = await uploadCover(game.id, file)
      setGame(updated)
    } catch (err) {
      if (err instanceof ApiError && err.body?.message) setError(err.body.message)
      else setError('No se pudo subir la portada.')
    } finally {
      setCoverUploading(false)
    }
  }

  if (loading) return <p className="text-slate-400 text-center mt-12">Cargando…</p>

  return (
    <div className="max-w-lg mx-auto">
      <h1 className="text-2xl font-bold mb-6">{isEdit ? 'Editar juego' : 'Nuevo juego'}</h1>

      {isEdit && game && (
        <div className="mb-6">
          <div className="aspect-video bg-slate-900 border border-slate-800 rounded-xl flex items-center justify-center overflow-hidden mb-2">
            {game.coverImageUrl ? (
              <img src={game.coverImageUrl} alt={game.title} className="w-full h-full object-cover" />
            ) : (
              <span className="text-slate-600 text-sm">Sin portada</span>
            )}
          </div>
          <label className="block">
            <span className="text-sm text-slate-300">Portada (PNG, JPEG o WEBP, máx. 5MB)</span>
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={handleCoverChange}
              disabled={coverUploading}
              className="mt-1 block w-full text-sm text-slate-400 file:mr-3 file:rounded-lg file:border-0 file:bg-slate-800 file:px-3 file:py-1.5 file:text-slate-200 hover:file:bg-slate-700"
            />
          </label>
          {coverUploading && <p className="text-xs text-slate-400 mt-1">Subiendo…</p>}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <FormField
          id="title"
          label="Título"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          error={fieldErrors.title}
          required
        />
        <div>
          <label htmlFor="description" className="block text-sm text-slate-300 mb-1">
            Descripción
          </label>
          <textarea
            id="description"
            rows={4}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className={`w-full rounded-lg bg-slate-800 border px-3 py-2 text-sm text-slate-100 outline-none transition-colors focus:border-indigo-500 ${
              fieldErrors.description ? 'border-red-500' : 'border-slate-700'
            }`}
            required
          />
          {fieldErrors.description && <p className="mt-1 text-xs text-red-400">{fieldErrors.description}</p>}
        </div>
        <FormField
          id="genre"
          label="Género"
          value={genre}
          onChange={(e) => setGenre(e.target.value)}
          error={fieldErrors.genre}
          required
        />
        <FormField
          id="price"
          label="Precio (USD)"
          type="number"
          min="0"
          step="0.01"
          value={price}
          onChange={(e) => setPrice(e.target.value)}
          error={fieldErrors.price}
          required
        />

        {error && (
          <p role="alert" className="text-sm text-red-400 rounded-lg bg-red-950/40 border border-red-900 px-3 py-2">
            {error}
          </p>
        )}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={submitting}
            className="flex-1 rounded-lg bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 py-2 text-sm font-medium transition-colors"
          >
            {submitting ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </form>
    </div>
  )
}
