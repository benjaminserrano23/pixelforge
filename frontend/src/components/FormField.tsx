import type { InputHTMLAttributes } from 'react'

// Input con label y mensaje de error por campo. Los errores vienen del
// backend con la forma { fields: { email: "must be a well-formed email" } },
// así que cada campo muestra el suyo debajo.

type Props = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  error?: string
}

export default function FormField({ label, error, id, ...inputProps }: Props) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm text-slate-300 mb-1">
        {label}
      </label>
      <input
        id={id}
        className={`w-full rounded-lg bg-slate-800 border px-3 py-2 text-sm text-slate-100 outline-none transition-colors focus:border-indigo-500 ${
          error ? 'border-red-500' : 'border-slate-700'
        }`}
        {...inputProps}
      />
      {error && <p className="mt-1 text-xs text-red-400">{error}</p>}
    </div>
  )
}
