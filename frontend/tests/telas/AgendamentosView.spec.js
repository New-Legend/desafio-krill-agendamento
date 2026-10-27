import { afterEach, describe, expect, it } from 'vitest'
import AgendamentosView from '../../src/views/AgendamentosView.vue'
import { hoje, somarDias } from '../../src/utils'
import { erroHttp, flushPromises, logar, montar, sair, simularApi } from '../montar'

const ag = (id, status, extra = {}) => ({
  id, data: hoje(), faixaHorariaId: 1, faixaHoraria: '08:00–10:00', idPedido: 900 + id, fornecedor: `Fornecedor ${id}`, emailFornecedor: 'f@f.com',
  tipoCaminhao: 'Truck', tipoPaletizacao: 'Paletizado', quantidadePaletes: 10, paletesEquivalentes: 10, status, criadoPor: 'admin', criadoEm: '2026-11-02T10:00:00-03:00', cdId: 1, ...extra
})
const dia = (d) => ({ data: d, capacidade: 480, paletesOcupados: 0, faixas: [1, 2, 3, 4].map((i) => ({ faixaHorariaId: i, descricao: `F${i}`, capacidade: 120, paletesOcupados: i === 2 ? 115 : 0 })) })
const botao = (w, texto) => w.findAll('button').find((b) => b.text().includes(texto))

function base(extra = {}) {
  return {
    'GET /cds': [{ id: 1, codigo: '806', nome: 'CD', ativo: true }],
    'GET /agendamentos': (r) => [ag(1, 'AGENDADO'), ag(2, 'CANCELADO'), ag(3, 'RECEBIDO', { paletesRecebidos: 9, divergencia: 'faltou 1' })].filter((a) => !r.params.status || a.status === r.params.status),
    'GET /agendamentos/1': ag(1, 'AGENDADO'),
    'GET /agendamentos/3': ag(3, 'RECEBIDO', { paletesRecebidos: 9, divergencia: 'faltou 1', chegadaEm: '2026-11-02T08:00:00-03:00' }),
    'GET /agendamentos/3/notas': [],
    'GET /ocupacao/periodo': () => [0, 1, 2].map((i) => dia(somarDias(hoje(), i))),
    ...extra
  }
}

describe('AgendamentosView', () => {
  afterEach(sair)

  it('lista com filtros e marca cancelados', async () => {
    logar('USUARIO')
    const api = simularApi(base())
    const { w } = await montar(AgendamentosView, { rota: '/agendamentos?status=AGENDADO' })
    expect(api.ultima('GET /agendamentos').params.status).toBe('AGENDADO')
    expect(w.text()).toContain('1 registro(s)')
    await w.find('#ffor').setValue(' Fornec ')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(api.ultima('GET /agendamentos').params.fornecedor).toBe('Fornec')
  })

  it('abre o detalhe pela rota; USUARIO não vê ações', async () => {
    logar('USUARIO')
    simularApi(base())
    const { w } = await montar(AgendamentosView, { rota: '/agendamentos/1' })
    expect(w.text()).toContain('Agendamento #1')
    expect(botao(w, 'Cancelar agendamento')).toBeUndefined()
    expect(botao(w, 'Remarcar')).toBeUndefined()
  })

  it('ADMIN cancela exigindo motivo', async () => {
    logar('ADMIN')
    const api = simularApi(base({ 'PATCH /agendamentos/1/cancelamento': (r) => ag(1, 'CANCELADO', { motivoCancelamento: r.corpo.motivo }) }))
    const { w } = await montar(AgendamentosView, { rota: '/agendamentos/1' })
    await botao(w, 'Cancelar agendamento').trigger('click')
    await botao(w, 'Confirmar cancelamento').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Informe o motivo')
    await w.find('#motivo').setValue('Fornecedor pediu')
    await botao(w, 'Confirmar cancelamento').trigger('click')
    await flushPromises()
    expect(api.ultima('PATCH /agendamentos/1/cancelamento').corpo).toEqual({ motivo: 'Fornecedor pediu' })
  })

  it('ADMIN registra chegada com placa', async () => {
    logar('ADMIN')
    const api = simularApi(base({ 'PATCH /agendamentos/1/status': ag(1, 'CHEGOU', { placa: 'ABC1D23' }), 'GET /agendamentos/1/notas': [] }))
    const { w } = await montar(AgendamentosView, { rota: '/agendamentos/1' })
    await botao(w, 'Registrar chegada').trigger('click')
    await w.find('#op-placa').setValue('abc1d23')
    await w.find('form.acao-op').trigger('submit')
    await flushPromises()
    expect(api.ultima('PATCH /agendamentos/1/status').corpo).toMatchObject({ acao: 'CHEGADA' })
  })

  it('ADMIN remarca escolhendo dia e faixa (faixa cheia continua permitida ao CD)', async () => {
    logar('ADMIN')
    const api = simularApi(base({ 'PATCH /agendamentos/1/data': (r) => ({ agendamento: ag(1, 'AGENDADO', { data: r.corpo.data, faixaHorariaId: r.corpo.faixaHorariaId }), alertas: [{ mensagem: 'Faixa excedida' }] }) }))
    const { w } = await montar(AgendamentosView, { rota: '/agendamentos/1' })
    await botao(w, 'Remarcar').trigger('click')
    await flushPromises()
    const amanha = somarDias(hoje(), 1)
    const cheia = w.findAll('.faixa').find((b) => b.attributes('aria-label').includes('F2') && b.classes('cheia'))
    expect(cheia.attributes('disabled')).toBeUndefined()
    expect(w.find('.faixa.atual').attributes('disabled')).toBeDefined()
    await w.findAll('.faixa')[5].trigger('click')
    await botao(w, 'Remarcar').trigger('click')
    await flushPromises()
    expect(api.ultima('PATCH /agendamentos/1/data').corpo).toEqual({ data: amanha, faixaHorariaId: 2 })
    expect(document.body.textContent).toContain('Faixa excedida')
  })

  it('recebido mostra divergência e o painel de NF-e', async () => {
    logar('ADMIN')
    simularApi(base())
    const { w } = await montar(AgendamentosView, { rota: '/agendamentos/3' })
    expect(w.text()).toContain('faltou 1')
    expect(w.text()).toContain('Notas fiscais (XML)')
  })

  it('agendamento inexistente volta para a lista', async () => {
    logar('ADMIN')
    simularApi(base({ 'GET /agendamentos/77': erroHttp(404, 'Agendamento não encontrado') }))
    const { router } = await montar(AgendamentosView, { rota: '/agendamentos/77' })
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/agendamentos')
  })
})
