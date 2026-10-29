import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import PatioView from '../../src/views/PatioView.vue'
import CapacidadeRecebimentoView from '../../src/views/CapacidadeRecebimentoView.vue'
import ItensAvulsos from '../../src/components/ItensAvulsos.vue'
import TiposPaleteView from '../../src/views/TiposPaleteView.vue'
import { hoje } from '../../src/utils'
import { menuItens } from '../../src/menu'
import { flushPromises, logar, montar, sair, simularApi } from '../montar'

const botao = (w, texto) => w.findAll('button').find((b) => b.text().includes(texto))
const cartao = (id, extra = {}) => ({ agendamentoId: id, fornecedor: 'Litoral', placa: `ABC1D2${id}`, faixa: '08:00 - 10:00', tipoCaminhao: 'Carreta', paletes: 10, modal: 'Sider / carreta', taxaDescarga: 250, ...extra })
const quadro = (lotado) => ({
  data: hoje(), cdId: 1,
  docas: [{ doca: { id: 1, nome: 'Doca 1' }, agendamentoId: lotado ? 3 : null, placa: lotado ? 'ABC1D23' : null, fornecedor: lotado ? 'Litoral' : null }],
  colunas: { PREVISTO: [cartao(1)], AGUARDANDO: [cartao(2, { transportadora: 'Transportes Sul', loja: 'Loja 7' })], NA_DOCA: lotado ? [cartao(3, { docaId: 1, doca: 'Doca 1' })] : [], DESCARREGANDO: [], LIBERADO: [] },
  indicador: { docasAtivas: 1, docasOcupadas: lotado ? 1 : 0, lotadoAgora: lotado, caminhoesNoPatio: lotado ? 2 : 1, diaCheio: lotado,
    dia: { capacidadePaletes: 100, paletes: lotado ? 120 : 40, capacidadeCaminhoes: 5, caminhoes: 2 } }
})

beforeEach(() => logar('ADMIN'))
afterEach(sair)

describe('pátio e docas', () => {
  it('mostra lotação, colunas e atribui doca', async () => {
    const api = simularApi({ 'GET /cds': [{ id: 1, codigo: 'CD1', nome: 'CD', ativo: true }], 'GET /patio': quadro(false), 'PUT /agendamentos/2/doca': { docaId: 1 } })
    const { w } = await montar(PatioView)
    expect(w.text()).toContain('Há docas livres')
    expect(w.text()).toContain('Ainda há capacidade no dia')
    expect(w.text()).toContain('Transportes Sul')
    expect(w.text()).toContain('Entrega na Loja 7')
    expect(w.text()).toContain('R$')
    const sel = w.findAllComponents({ name: 'Select' }).find((s) => s.props('placeholder') === 'Atribuir doca')
    sel.vm.$emit('change', { value: 1 })
    await flushPromises()
    expect(api.ultima('PUT /agendamentos/2/doca').corpo).toEqual({ docaId: 1 })
  })

  it('CD lotado e liberação de doca', async () => {
    const api = simularApi({ 'GET /cds': [], 'GET /patio': quadro(true), 'PUT /agendamentos/3/doca': { docaId: null } })
    const { w } = await montar(PatioView)
    expect(w.text()).toContain('CD lotado')
    expect(w.text()).toContain('Capacidade do dia atingida')
    await botao(w, 'Liberar doca').trigger('click')
    await flushPromises()
    expect(api.ultima('PUT /agendamentos/3/doca').corpo).toEqual({ docaId: null })
  })

  it('menu traz pátio e os novos cadastros', () => {
    const tudo = menuItens(true).flatMap((g) => g.itens.map((i) => i.to))
    expect(tudo).toEqual(expect.arrayContaining(['/patio', '/cadastros/docas', '/cadastros/capacidade-recebimento', '/cadastros/taxas-descarga']))
  })
})

describe('capacidade por CD e loja', () => {
  it('lista regras e responde se está cheio', async () => {
    const api = simularApi({
      'GET /cds': [{ id: 1, codigo: 'CD1', nome: 'CD Principal', ativo: true }],
      'GET /capacidades-recebimento': [{ id: 1, cdId: null, loja: 'Loja 7', diaSemana: 1, paletes: 20, caminhoes: 2 }],
      'GET /capacidades-recebimento/ocupacao': (r) => (r.params.loja
        ? { destino: 'a loja Loja 7', limitado: true, cheio: true, capacidadePaletes: 20, paletes: 25, percentualPaletes: 125, capacidadeCaminhoes: 2, caminhoes: 2, percentualCaminhoes: 100 }
        : { destino: 'o CD CD1', limitado: true, cheio: false, capacidadePaletes: 480, paletes: 100, percentualPaletes: 20.8, caminhoes: 3 })
    })
    const { w } = await montar(CapacidadeRecebimentoView)
    expect(w.text()).toContain('Segunda')
    expect(w.text()).toContain('Não, o CD CD1 ainda tem vaga')
    await w.find('#cq-loja').setValue('Loja 7')
    await flushPromises()
    expect(w.text()).toContain('Sim, a loja Loja 7 está cheio')
    expect(api.ultima('GET /capacidades-recebimento/ocupacao').params.loja).toBe('Loja 7')
  })
})

describe('produto avulso e configuração do vale', () => {
  it('lança produto avulso', async () => {
    const api = simularApi({ 'GET /agendamentos/5/itens-avulsos': [], 'POST /agendamentos/5/itens-avulsos': (r) => ({ id: 1, ...r.corpo }) })
    const { w } = await montar(ItensAvulsos, { props: { agendamentoId: 5, admin: true } })
    expect(w.text()).toContain('Nenhum produto avulso')
    await botao(w, 'Adicionar produto avulso').trigger('click')
    await w.find('#ia-cod').setValue('BRINDE1')
    await w.find('#ia-desc').setValue('Display')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(api.ultima('POST /agendamentos/5/itens-avulsos').corpo).toMatchObject({ codigo: 'BRINDE1', descricao: 'Display', quantidade: 1, cadastrarProduto: true })
  })

  it('salva exigir as duas assinaturas', async () => {
    const api = simularApi({
      'GET /cds': [], 'GET /tipos-palete': [],
      'GET /vales-palete/configuracao': { exigirDuasAssinaturas: false, agrupamento: 'fornecedor' },
      'PUT /vales-palete/configuracao': (r) => r.corpo
    })
    const { w } = await montar(TiposPaleteView)
    expect(w.text()).toContain('Exigir as duas assinaturas')
    await w.find('#cfg-ass').setValue(true)
    await botao(w, 'Salvar configurações').trigger('click')
    await flushPromises()
    expect(api.ultima('PUT /vales-palete/configuracao').corpo).toEqual({ exigirDuasAssinaturas: true, agrupamento: 'fornecedor' })
  })
})
