import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, clearToken, getToken, setToken } from './client'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('api()', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('adjunta el token guardado como Authorization header', async () => {
    setToken('TOKEN123')
    vi.mocked(fetch).mockResolvedValue(jsonResponse(200, { ok: true }))

    await api('/api/games/mine')

    const [, options] = vi.mocked(fetch).mock.calls[0]
    expect((options?.headers as Record<string, string>).Authorization).toBe('Bearer TOKEN123')
  })

  it('no envía Authorization cuando no hay token', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(200, { ok: true }))

    await api('/api/games')

    const [, options] = vi.mocked(fetch).mock.calls[0]
    expect((options?.headers as Record<string, string>).Authorization).toBeUndefined()
  })

  it('lanza ApiError con el cuerpo del backend cuando la respuesta no es 2xx', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(409, { error: 'already_purchased' }))

    await expect(api('/api/games/1/purchase', { method: 'POST' })).rejects.toMatchObject({
      status: 409,
      body: { error: 'already_purchased' },
    })
  })

  it('limpia el token guardado cuando el backend responde 401', async () => {
    setToken('EXPIRED')
    vi.mocked(fetch).mockResolvedValue(jsonResponse(401, { error: 'invalid_credentials' }))

    await expect(api('/api/auth/me')).rejects.toBeInstanceOf(ApiError)
    expect(getToken()).toBeNull()
  })

  it('no toca el token si el 401 ocurre sin haber uno guardado (login fallido)', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(401, { error: 'invalid_credentials' }))

    await expect(api('/api/auth/login', { method: 'POST', body: {} })).rejects.toBeInstanceOf(ApiError)
    expect(getToken()).toBeNull()
  })

  it('clearToken deja getToken en null', () => {
    setToken('X')
    clearToken()
    expect(getToken()).toBeNull()
  })
})
