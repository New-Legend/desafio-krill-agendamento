import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import NovoAgendamentoView from '../../src/views/NovoAgendamentoView.vue'
import { hoje } from '../../src/utils'
import { flushPromises, logar, montar, sair, simularApi, erroHttp } from '../montar'

const ocupacao = {
  data: hoje(), capacidade: 480, paletesOcupados: 100, percentual: 20.8, excedente: 0, quantidadeAgendamentos: 2,
  faixas: [{ faixaHorariaId: 1, descricao: '08:00–10:00', capacidade: 120, paletesOcupados: 100, percentual: 83.3, excedente: 0 },
    { faixaHorariaId: 2, descricao: '10:00–12:00', capacidade: 120, paletesOcupados: 0, percentual: 0, excedente: 0 }]
}
function rotas(extra = {}) {
  return {
    'GET /tipos-caminhao': [{ id: 1, nome: 'Truck', ativo: true }, { id: 9, nome: 'Velho', ativo: false }],
    'GET /tipos-paletizacao': [{ id: 1, nome: 'Paletizado', fatorMultiplicador: 1, ativo: true }, { id: 2, nome: 'Não paletizado', fatorMultiplicador: 2, ativo: true }],
    'GET /fornecedores': [{ id: 7, cnpj: '11222333000181', razaoSocial: 'Litoral Alimentos Ltda', nomeFantasia: 'Litoral', email: 'agenda@litoral.com.br', ativo: true }],
    'GET /cds': [{ id: 1, codigo: '806', nome: 'CD Rede Krill', ativo: true }, { id: 2, codigo: '807', nome: 'CD 2', ativo: true }],
    'POST /agendamentos/simulacao': (r) => {
      const fator = r.corpo.tipoPaletizacaoId === 2 ? 2 : 1
      const eq = (r.corpo.quantidadePaletes || 0) * fator
      return { ocupacaoAtual: ocupacao, paletesEquivalentes: eq, alertas: eq > 20 && r.corpo.faixaHorariaId === 1 ? [{ tipo: 'FAIXA', mensagem: 'Faixa 08:00–10:00 excede em 20 paletes' }] : [] }
    },
    'GET /erp/pedidos/:id': { provedor: 'local', pedido: { situacao: 'Aberto', fornecedorNome: 'Fornecedor ERP', fornecedorEmail: 'erp@f.com', quantidadePaletesPrevista: 12 }, tipoPaletizacaoId: 1 },
    ...extra
  }
}
const selects = (w) => Object.fromEntries(w.findAllComponents(Select).map((s) => [s.props('inputId'), s]))
async function escolher(w, id, valor) {
  selects(w)[id].vm.$emit('update:modelValue', valor)
  await flushPromises()
}

describe('NovoAgendamentoView', () => {
  beforeEach(() => logar('ADMIN'))
  afterEach(sair)

  it('valida todos os campos obrigatórios sem chamar a API', async () => {
    const api = simularApi(rotas())
    const { w } = await montar(NovoAgendamentoView, { rota: '/agendamentos/novo' })
    await w.find('form').trigger('submit')
    await flushPromises()
    const t = w.text()
    for (const m of ['Selecione a faixa horária', 'Informe o ID do pedido', 'Informe o fornecedor', 'Informe o e-mail', 'Selecione o tipo de caminhão', 'Selecione o tipo de paletização', 'Informe a quantidade de paletes']) expect(t).toContain(m)
    expect(api.chamadas.some((c) => c.chave === 'POST /agendamentos')).toBe(false)
  })

  it('e-mail inválido é barrado', async () => {
    simularApi(rotas())
    const { w } = await montar(NovoAgendamentoView)
    await w.find('#email').setValue('sem-arroba')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('E-mail inválido')
  })

  it('preenche pelo fornecedor cadastrado e pelo pedido do ERP, simula ×2 e envia o agendamento', async () => {
    const api = simularApi(rotas({ 'POST /agendamentos': (r) => ({ agendamento: { id: 55, emailFornecedor: r.corpo.emailFornecedor }, alertas: [{ tipo: 'FAIXA', mensagem: 'excede' }] }) }))
    const { w, router } = await montar(NovoAgendamentoView)
    expect(selects(w).cd).toBeTruthy()
    await escolher(w, 'fcad', 7)
    expect(w.find('#fornecedor').element.value).toBe('Litoral')
    expect(w.find('#email').element.value).toBe('agenda@litoral.com.br')

    const pedido = w.findAllComponents(InputNumber).find((c) => c.props('inputId') === 'pedido')
    pedido.vm.$emit('update:modelValue', 4512)
    await flushPromises()
    await w.find('#pedido').trigger('blur')
    await flushPromises()
    expect(w.find('#fornecedor').element.value).toBe('Fornecedor ERP')
    expect(w.text()).toContain('base local')

    await escolher(w, 'cd', 2)
    await escolher(w, 'faixa', 1)
    await escolher(w, 'caminhao', 1)
    await escolher(w, 'paletizacao', 2)
    expect(api.ultima('POST /agendamentos/simulacao').corpo).toMatchObject({ cdId: 2, tipoPaletizacaoId: 2, quantidadePaletes: 12 })
    expect(w.text()).toContain('conta como 24 paletes')
    expect(w.text()).toContain('excede em 20 paletes')

    await w.find('form').trigger('submit')
    await flushPromises()
    const corpo = api.ultima('POST /agendamentos').corpo
    expect(corpo).toMatchObject({ idPedido: 4512, fornecedor: 'Fornecedor ERP', emailFornecedor: 'erp@f.com', faixaHorariaId: 1, tipoCaminhaoId: 1, tipoPaletizacaoId: 2, quantidadePaletes: 12, cdId: 2, fornecedorId: 7 })
    expect(corpo.data).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(router.currentRoute.value.path).toBe('/agendamentos/55')
  })

  it('mostra erro do servidor e não navega', async () => {
    simularApi(rotas({ 'POST /agendamentos': erroHttp(422, 'Data no passado') }))
    const { w, router } = await montar(NovoAgendamentoView, { rota: '/agendamentos/novo' })
    await w.find('#fornecedor').setValue('F')
    await w.find('#email').setValue('f@f.com')
    for (const [id, v] of [['faixa', 2], ['caminhao', 1], ['paletizacao', 1]]) await escolher(w, id, v)
    for (const [id, v] of [['pedido', 1], ['qtd', 3]]) {
      w.findAllComponents(InputNumber).find((c) => c.props('inputId') === id).vm.$emit('update:modelValue', v)
    }
    await flushPromises()
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(document.body.textContent).toContain('Data no passado')
    expect(router.currentRoute.value.path).toBe('/agendamentos/novo')
  })
})
