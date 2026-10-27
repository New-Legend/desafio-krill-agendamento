import { mount, flushPromises } from '@vue/test-utils'
import { h } from 'vue'
import Toast from 'primevue/toast'
import { createRouter, createMemoryHistory } from 'vue-router'
import { vi } from 'vitest'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import { registrarComponentes } from '../src/componentes'
import { iniciarSessao, encerrarSessao } from '../src/sessao'

export { flushPromises }

/**
 * Simula a API: rotas = { 'GET /agendamentos': resposta | (req) => resposta }.
 * Resposta { status, corpo } força erro HTTP. Guarda as chamadas em `chamadas`.
 */
export function simularApi(rotas) {
  const chamadas = []
  const fetch = vi.fn(async (url, opts = {}) => {
    const u = new URL(url, 'http://x')
    const caminho = u.pathname.replace('/api/v1', '')
    const chave = `${opts.method || 'GET'} ${caminho}`
    const corpo = typeof opts.body === 'string' ? JSON.parse(opts.body) : opts.body
    const req = { chave, caminho, params: Object.fromEntries(u.searchParams), corpo, headers: opts.headers }
    chamadas.push(req)
    let r = rotas[chave]
    if (r === undefined) r = Object.entries(rotas).find(([k]) => new RegExp(`^${k.replace(/:\w+/g, '[^/]+')}$`).test(chave))?.[1]
    if (typeof r === 'function') r = r(req)
    if (r === undefined) return resposta(404, { mensagem: `sem mock: ${chave}` })
    if (r && r.__erro) return resposta(r.status, r.corpo)
    return resposta(r === null ? 204 : 200, r)
  })
  vi.stubGlobal('fetch', fetch)
  return { chamadas, fetch, ultima: (chave) => [...chamadas].reverse().find((c) => c.chave === chave) }
}

export const erroHttp = (status, mensagem) => ({ __erro: true, status, corpo: { mensagem } })

function resposta(status, corpo) {
  return {
    ok: status < 400,
    status,
    json: async () => corpo,
    blob: async () => new Blob([JSON.stringify(corpo)])
  }
}

export function logar(perfil = 'ADMIN') {
  iniciarSessao({ token: 't', usuario: perfil.toLowerCase(), nome: perfil, perfil, expiraEmSegundos: 3600 })
}
export const sair = encerrarSessao

export async function montar(componente, { props, rota = '/', rotas, stubs } = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: rotas || ['/agendamentos/:id(\\d+)', '/fornecedor/:token', '/paletes/recibos/:id', '/vales-palete/:id(\\d+)', '/:p(.*)*']
      .map((path) => ({ path, component: { render: () => null } }))
  })
  router.push(rota)
  await router.isReady()
  const Raiz = { render: () => h('div', [h(Toast), h(componente, props)]) }
  const w = mount(Raiz, {
    attachTo: document.body,
    global: {
      plugins: [router, [PrimeVue, { theme: 'none' }], ToastService, { install: registrarComponentes }],
      stubs: { teleport: true, ...stubs }
    }
  })
  await flushPromises()
  return { w, router, tela: w.findComponent(componente), toast: () => document.body.textContent }
}
