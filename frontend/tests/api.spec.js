import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, quandoSessaoExpirar } from '../src/api'
import { iniciarSessao, sessao, isAdmin, encerrarSessao } from '../src/sessao'

const resposta = (status, corpo) => ({ ok: status < 400, status, json: async () => corpo, blob: async () => 'b' })

describe('api', () => {
  beforeEach(() => { encerrarSessao(); vi.restoreAllMocks() })

  it('envia token e monta query sem vazios', async () => {
    iniciarSessao({ token: 't1', usuario: 'admin', nome: 'Adm', perfil: 'ADMIN', expiraEmSegundos: 60 })
    expect(isAdmin.value).toBe(true)
    const f = vi.fn().mockResolvedValue(resposta(200, [1]))
    vi.stubGlobal('fetch', f)
    expect(await api('/agendamentos', { params: { inicio: '2026-11-01', status: null, fornecedor: '' } })).toEqual([1])
    expect(f.mock.calls[0][0]).toBe('/api/v1/agendamentos?inicio=2026-11-01')
    expect(f.mock.calls[0][1].headers.Authorization).toBe('Bearer t1')
  })

  it('trata erros e sessão expirada', async () => {
    iniciarSessao({ token: 't', usuario: 'u', nome: 'U', perfil: 'USUARIO', expiraEmSegundos: 60 })
    const expirou = vi.fn()
    quandoSessaoExpirar(expirou)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(422, { mensagem: 'Regra', detalhes: ['x'] })))
    await expect(api('/x', { method: 'POST', body: {} })).rejects.toMatchObject({ status: 422, message: 'Regra' })
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 403, json: async () => { throw new Error() } }))
    await expect(api('/x')).rejects.toMatchObject({ message: 'Acesso negado para o seu perfil' })
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(401, {})))
    await expect(api('/x')).rejects.toBeInstanceOf(ApiError)
    expect(expirou).toHaveBeenCalled()
    expect(sessao.atual).toBeNull()
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('rede')))
    await expect(api('/x')).rejects.toMatchObject({ status: 0 })
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(204)))
    expect(await api('/x')).toBeNull()
  })
})
