import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import FornecedorPublicoView from '../../src/views/FornecedorPublicoView.vue'
import DivergenciasView from '../../src/views/DivergenciasView.vue'
import DescargaView from '../../src/views/DescargaView.vue'
import RotinaView from '../../src/views/RotinaView.vue'
import ContaCorrenteView from '../../src/views/ContaCorrenteView.vue'
import ReciboPaleteView from '../../src/views/ReciboPaleteView.vue'
import MovimentosPaleteView from '../../src/views/MovimentosPaleteView.vue'
import NotasFiscais from '../../src/components/NotasFiscais.vue'
import { hoje, somarDias } from '../../src/utils'
import { erroHttp, flushPromises, logar, montar, sair, simularApi } from '../montar'

const botao = (w, texto) => w.findAll('button').find((b) => b.text().includes(texto))
const dia = (d, ocupadoF1 = 0) => ({ data: d, capacidade: 480, paletesOcupados: ocupadoF1, faixas: [1, 2].map((i) => ({ faixaHorariaId: i, descricao: `F${i}`, capacidade: 120, paletesOcupados: i === 1 ? ocupadoF1 : 0 })) })
const agAg = { id: 9, data: hoje(), faixaHorariaId: 1, faixaHoraria: 'F1', idPedido: 1, fornecedor: 'Litoral', tipoCaminhao: 'Truck', tipoPaletizacao: 'Paletizado', quantidadePaletes: 20, paletesEquivalentes: 20, status: 'AGENDADO', editavel: true }
const agRow = (id, extra = {}) => ({ agendamento: { id, data: hoje(), faixaHoraria: 'F1', idPedido: id, fornecedor: 'Litoral', tipoCaminhao: 'Truck', quantidadePaletes: 10, paletesRecebidos: 8, divergencia: 'faltaram 2', placa: 'ABC1D23', ...extra }, diferencaPaletes: -2, notas: 1, pesoNfDivergente: true })

beforeEach(() => {
  globalThis.URL.createObjectURL = vi.fn(() => 'blob:x')
  globalThis.URL.revokeObjectURL = vi.fn()
})
afterEach(sair)

describe('página pública do fornecedor', () => {
  it('fornecedor remarca só em faixa com vaga', async () => {
    const amanha = somarDias(hoje(), 1)
    const api = simularApi({
      'GET /publico/fornecedor/tok': agAg,
      'GET /publico/fornecedor/tok/disponibilidade': [dia(hoje(), 0), dia(amanha, 110)],
      'PUT /publico/fornecedor/tok/reagendamento': (r) => ({ ...agAg, data: r.corpo.data, faixaHorariaId: r.corpo.faixaHorariaId })
    })
    const { w } = await montar(FornecedorPublicoView, { rota: '/fornecedor/tok' })
    expect(w.find('img[alt="Rede Krill"]').exists()).toBe(true)
    await botao(w, 'Escolher outra data').trigger('click')
    await flushPromises()
    const faixas = w.findAll('.faixa')
    expect(faixas[0].attributes('disabled')).toBeDefined() // atual
    expect(faixas[2].attributes('disabled')).toBeDefined() // amanhã F1 sem vaga (110 + 20 > 120)
    expect(faixas[2].text()).toContain('sem vaga')
    await faixas[3].trigger('click')
    expect(w.text()).toContain('Nova entrega')
    await botao(w, 'Confirmar nova data').trigger('click')
    await flushPromises()
    expect(api.ultima('PUT /publico/fornecedor/tok/reagendamento').corpo).toEqual({ data: amanha, faixaHorariaId: 2 })
  })

  it('link inválido', async () => {
    simularApi({ 'GET /publico/fornecedor/x': erroHttp(404, 'não') })
    const { w } = await montar(FornecedorPublicoView, { rota: '/fornecedor/x' })
    expect(w.text()).toContain('Link inválido ou expirado')
  })

  it('confirma presença e envia veículo', async () => {
    const api = simularApi({ 'GET /publico/fornecedor/tok': agAg, 'POST /publico/fornecedor/tok/confirmacao': { ...agAg, status: 'CONFIRMADO' }, 'PUT /publico/fornecedor/tok/veiculo': (r) => ({ ...agAg, ...r.corpo }) })
    const { w } = await montar(FornecedorPublicoView, { rota: '/fornecedor/tok' })
    await botao(w, 'Confirmar presença').trigger('click')
    await flushPromises()
    await w.find('#pl').setValue(' ABC1D23 ')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(api.ultima('PUT /publico/fornecedor/tok/veiculo').corpo).toEqual({ placa: 'ABC1D23', motorista: null })
  })
})

describe('relatórios do recebimento', () => {
  beforeEach(() => logar('USUARIO'))

  it('divergências: busca, valida período e exporta CSV', async () => {
    const api = simularApi({ 'GET /divergencias': [agRow(1)], 'GET /divergencias.csv': 'csv', 'GET /cds': [] })
    const { w, tela } = await montar(DivergenciasView)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('faltaram 2')
    expect(w.text()).toContain('-2')
    await botao(w, 'Exportar CSV').trigger('click')
    await flushPromises()
    expect(api.ultima('GET /divergencias.csv').params.inicio).toMatch(/^\d{4}-/)
    tela.vm.filtro.fim = null
    await tela.vm.buscar()
    expect(document.body.textContent).toContain('Informe o período')
  })

  it('descarga: resumo e situação', async () => {
    simularApi({
      'GET /cds': [],
      'GET /relatorios/descarga': { resumo: [{ tipoCaminhao: 'Truck', recebimentos: 1, mediaEsperaMinutos: 30, mediaDescargaMinutos: 95, previstoMinutos: 60, acimaDoPrevisto: 1, custoTotal: 63 }], linhas: [{ agendamento: agRow(1).agendamento, indicador: { esperaMinutos: 30, descargaMinutos: 95, excessoMinutos: 35, esperaAcimaTolerancia: false, custo: 63, situacao: 'ACIMA_DO_PREVISTO' } }] },
      'GET /relatorios/descarga.csv': 'x'
    })
    const { w } = await montar(DescargaView)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('1h35')
    expect(w.text()).toContain('Acima do previsto')
    await botao(w, 'Exportar CSV').trigger('click')
  })

  it('rotina do dia: contadores e itens', async () => {
    simularApi({ 'GET /cds': [], 'GET /rotina': { data: hoje(), itens: [{ codigo: 'ATRASADOS', titulo: 'Caminhões atrasados', nivel: 'erro', itens: [{ id: 4, descricao: 'Pedido 4 · Litoral', link: '/agendamentos/4' }] }, { codigo: 'VALES', titulo: 'Vales pendentes', nivel: 'aviso', itens: [] }] } })
    const { w, router } = await montar(RotinaView)
    expect(w.text()).toContain('Caminhões atrasados')
    expect(w.text()).toContain('Tudo em dia')
    await w.find('a').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/agendamentos/4')
  })
})

describe('paletes', () => {
  beforeEach(() => logar('ADMIN'))
  const saldo = { fornecedor: 'Bebidas Vale', vales: 2, entregues: 25, devolvidos: 5, cobrados: 0, pagos: 0, saldoEmAberto: 20, valorEmAberto: 0 }
  const cob = { id: 5, fornecedor: 'Bebidas Vale', quantidade: 10, valorUnitario: 45, valorTotal: 450, vencimento: hoje(), vencida: false, status: 'ABERTA' }

  it('conta-corrente: cobra dentro do saldo', async () => {
    const api = simularApi({ 'GET /paletes/saldos': [saldo], 'GET /paletes/cobrancas': [cob], 'POST /paletes/cobrancas': { id: 6 } })
    const { w, tela } = await montar(ContaCorrenteView, { rota: '/paletes/saldos' })
    expect(w.text()).toContain('20 paletes em aberto')
    await botao(w, 'Cobrar').trigger('click')
    tela.vm.dlg.quantidade = 30
    await tela.vm.confirmar()
    expect(tela.vm.erro).toContain('entre 1 e 20')
    tela.vm.dlg.quantidade = 10
    await tela.vm.confirmar()
    await flushPromises()
    expect(api.ultima('POST /paletes/cobrancas').corpo).toMatchObject({ fornecedor: 'Bebidas Vale', quantidade: 10, valorUnitario: 45 })
  })

  it('cobranças: receber gera recibo; cancelar exige motivo', async () => {
    const api = simularApi({ 'GET /paletes/saldos': [], 'GET /paletes/cobrancas': [cob], 'POST /paletes/cobrancas/5/pagamento': { ...cob, id: 5, reciboNumero: 'RP-20261102-0001' }, 'PATCH /paletes/cobrancas/5/cancelamento': { ...cob, status: 'CANCELADA' } })
    const { w, tela, router } = await montar(ContaCorrenteView, { rota: '/paletes/cobrancas' })
    expect(w.text()).toContain('Aberta')
    tela.vm.abrir('cancelar', cob)
    await tela.vm.confirmar()
    expect(tela.vm.erro).toBe('Informe o motivo')
    tela.vm.dlg.motivo = 'acordo'
    await tela.vm.confirmar()
    await flushPromises()
    expect(api.ultima('PATCH /paletes/cobrancas/5/cancelamento').corpo).toEqual({ motivo: 'acordo' })
    await botao(w, 'Receber').trigger('click')
    tela.vm.dlg.forma = 'BOLETO'
    await tela.vm.confirmar()
    await flushPromises()
    expect(api.ultima('POST /paletes/cobrancas/5/pagamento').corpo).toEqual({ forma: 'BOLETO' })
    expect(router.currentRoute.value.path).toBe('/paletes/recibos/5')
  })

  it('recibo impresso com logo', async () => {
    simularApi({ 'GET /paletes/cobrancas/5': { ...cob, status: 'PAGA', reciboNumero: 'RP-20261102-0001', formaPagamento: 'PIX', pagoEm: '2026-11-02T10:00:00-03:00', recebidoPor: 'admin' } })
    window.print = vi.fn()
    const { w } = await montar(ReciboPaleteView, { rota: '/paletes/recibos/5' })
    expect(w.text()).toContain('RP-20261102-0001')
    expect(w.find('img[alt="Rede Krill"]').attributes('width')).toBe('570')
    await botao(w, 'Imprimir').trigger('click')
    expect(window.print).toHaveBeenCalled()
  })

  it('paletes nas lojas: visão por loja e novo romaneio', async () => {
    const api = simularApi({ 'GET /paletes/lojas': [{ loja: 'Loja 12', pbr: 18, chep: 0, chapatex: 0, descartaveis: 0, total: 18, ultimoMovimento: hoje() }], 'GET /paletes/movimentos': [{ id: 1, romaneio: 'RM-1', data: hoje(), tipo: 'ENVIO', loja: 'Loja 12', quantidades: { pbr: 30, chep: 0, chapatex: 0, descartaveis: 0 }, total: 30 }], 'POST /paletes/movimentos': { romaneio: 'RM-2' } })
    const { w, tela } = await montar(MovimentosPaleteView)
    expect(w.text()).toContain('Loja 12')
    expect(w.text()).toContain('RM-1')
    await botao(w, 'Movimentar').trigger('click')
    tela.vm.form.pbr = 0
    await tela.vm.salvar()
    expect(tela.vm.erros.total).toBeTruthy()
    tela.vm.form.tipo = 'RETORNO'
    tela.vm.form.pbr = 12
    await tela.vm.salvar()
    await flushPromises()
    expect(api.ultima('POST /paletes/movimentos').corpo).toMatchObject({ tipo: 'RETORNO', loja: 'Loja 12', pbr: 12 })
  })
})

describe('NF-e no detalhe do agendamento', () => {
  it('importa XML e mostra avisos e itens', async () => {
    logar('ADMIN')
    const nota = { id: 1, numero: '12345', serie: '1', emitenteNome: 'Litoral', chave: '3526', volumes: 150, pesoBrutoKg: 3600, pesoCalculadoKg: 520, pesoDivergente: true, itensSemPeso: 1, itens: [{ numeroItem: 1, codigo: 'ARZ5', descricao: 'Arroz', quantidade: 100, unidade: 'FD', pesoKg: 520 }] }
    let notas = []
    const api = simularApi({ 'GET /agendamentos/3/notas': () => notas, 'POST /agendamentos/3/notas': () => ((notas = [nota]), { nota, produtosCriados: 1, avisos: ['Produto FEJ1 sem peso cadastrado'] }), 'DELETE /notas/1': () => ((notas = []), null) })
    const { w, tela } = await montar(NotasFiscais, { props: { agendamentoId: 3, admin: true } })
    expect(w.text()).toContain('Nenhuma NF-e importada')
    const arquivo = new File(['<nfeProc/>'], 'nfe.xml', { type: 'text/xml' })
    await tela.vm.importar({ target: { files: [arquivo], value: 'x' } })
    await flushPromises()
    expect(api.ultima('POST /agendamentos/3/notas').corpo).toBeInstanceOf(FormData)
    expect(w.text()).toContain('FEJ1 sem peso')
    expect(w.text()).toContain('peso divergente')
    expect(w.text()).toContain('Arroz')
    await botao(w, 'Remover NF-e').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Nenhuma NF-e importada')
    const grande = { size: 3 * 1024 * 1024 }
    await tela.vm.importar({ target: { files: [grande], value: 'x' } })
    expect(document.body.textContent).toContain('XML acima de 2 MB')
  })
})
