import { afterEach, describe, expect, it } from 'vitest'
import LoginView from '../../src/views/LoginView.vue'
import { sessao } from '../../src/sessao'
import { erroHttp, flushPromises, montar, sair, simularApi } from '../montar'

describe('LoginView', () => {
  afterEach(sair)

  it('exige usuário e senha', async () => {
    simularApi({})
    const { w } = await montar(LoginView)
    expect(w.find('h1').text()).toBe('Agendamento Rede Krill')
    await w.find('form').trigger('submit')
    expect(w.text()).toContain('Informe usuário e senha')
  })

  it('entra, guarda a sessão e volta para a rota pedida', async () => {
    const api = simularApi({ 'POST /auth/login': { token: 'jwt', usuario: 'admin', nome: 'Admin', perfil: 'ADMIN', expiraEmSegundos: 60 } })
    const { w, router } = await montar(LoginView, { rota: '/login?voltar=/relatorio' })
    await w.find('input[type="text"], input:not([type])').setValue(' admin ')
    await w.find('input[type="password"]').setValue('Admin@123')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(api.ultima('POST /auth/login').corpo).toEqual({ usuario: 'admin', senha: 'Admin@123' })
    expect(sessao.atual.perfil).toBe('ADMIN')
    expect(router.currentRoute.value.fullPath).toBe('/relatorio')
  })

  it('mostra o erro do servidor', async () => {
    simularApi({ 'POST /auth/login': erroHttp(401, 'Usuário ou senha inválidos') })
    const { w } = await montar(LoginView)
    await w.find('input:not([type="password"])').setValue('x')
    await w.find('input[type="password"]').setValue('y')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('Usuário ou senha inválidos')
  })
})
