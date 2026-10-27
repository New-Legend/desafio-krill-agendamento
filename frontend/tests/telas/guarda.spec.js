import { afterEach, describe, expect, it } from 'vitest'
import { guarda, routes } from '../../src/router'
import { logar, sair } from '../montar'

const rota = (path) => {
  const r = routes.find((x) => x.path === path)
  return { name: r.name, meta: r.meta || {}, fullPath: path }
}

describe('guarda de rota', () => {
  afterEach(sair)

  it('manda para o login quem não está logado, guardando a volta', () => {
    expect(guarda(rota('/agendamentos'))).toEqual({ name: 'login', query: { voltar: '/agendamentos' } })
  })
  it('libera telas públicas (login e link do fornecedor)', () => {
    expect(guarda(rota('/login'))).toBe(true)
    expect(guarda(rota('/fornecedor/:token'))).toBe(true)
  })
  it('usuário logado não volta ao login', () => {
    logar('USUARIO')
    expect(guarda(rota('/login'))).toEqual({ name: 'inicio' })
  })
  it('USUARIO não entra em telas de ADMIN', () => {
    logar('USUARIO')
    for (const p of ['/agendamentos/novo', '/cadastros/fornecedores', '/cadastros/cds', '/cadastros/produtos', '/auditoria']) {
      expect(guarda(rota(p))).toEqual({ name: 'inicio' })
    }
    expect(guarda(rota('/agendamentos'))).toBe(true)
    expect(guarda(rota('/paletes/saldos'))).toBe(true)
  })
  it('ADMIN entra em tudo', () => {
    logar('ADMIN')
    expect(guarda(rota('/cadastros/fornecedores'))).toBe(true)
    expect(guarda(rota('/agendamentos/novo'))).toBe(true)
  })
  it('sessão expirada conta como deslogado', () => {
    logar('ADMIN')
    localStorage.clear()
    sair()
    expect(guarda(rota('/painel')).name).toBe('login')
  })
})
