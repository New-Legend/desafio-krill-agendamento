import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import InputNumber from 'primevue/inputnumber'
import FornecedoresView from '../../src/views/FornecedoresView.vue'
import CentrosDistribuicaoView from '../../src/views/CentrosDistribuicaoView.vue'
import ProdutosView from '../../src/views/ProdutosView.vue'
import TiposCaminhaoView from '../../src/views/TiposCaminhaoView.vue'
import TiposPaletizacaoView from '../../src/views/TiposPaletizacaoView.vue'
import FaixasView from '../../src/views/FaixasView.vue'
import ParametrosDescargaView from '../../src/views/ParametrosDescargaView.vue'
import { erroHttp, flushPromises, logar, montar, sair, simularApi } from '../montar'

const botao = (w, texto) => w.findAll('button').find((b) => b.text().includes(texto))
const fornecedor = { id: 3, cnpj: '11222333000181', razaoSocial: 'Litoral Alimentos', nomeFantasia: 'Litoral', email: 'a@litoral.com', telefone: null, contato: null, ativo: true }

describe('cadastros', () => {
  beforeEach(() => logar('ADMIN'))
  afterEach(sair)

  it('fornecedores: lista com CNPJ formatado, valida CNPJ/e-mail e inclui', async () => {
    const api = simularApi({ 'GET /fornecedores': [fornecedor], 'POST /fornecedores': (r) => ({ id: 4, ...r.corpo }) })
    const { w } = await montar(FornecedoresView)
    expect(w.text()).toContain('11.222.333/0001-81')
    await botao(w, 'Novo fornecedor').trigger('click')
    await botao(w, 'Salvar').trigger('click')
    expect(w.text()).toContain('Informe cnpj')
    await w.find('#cnpj').setValue('11.222.333/0001-80')
    await w.find('#razaoSocial').setValue('Nova Ltda')
    await w.find('#email').setValue('invalido')
    await botao(w, 'Salvar').trigger('click')
    expect(w.text()).toContain('CNPJ inválido')
    expect(w.text()).toContain('E-mail inválido')
    await w.find('#cnpj').setValue('11.222.333/0001-81')
    await w.find('#email').setValue('compras@nova.com')
    await botao(w, 'Salvar').trigger('click')
    await flushPromises()
    expect(api.ultima('POST /fornecedores').corpo).toMatchObject({ cnpj: '11.222.333/0001-81', razaoSocial: 'Nova Ltda', email: 'compras@nova.com', ativo: true })
    expect(api.ultima('POST /fornecedores').corpo.id).toBeUndefined()
  })

  it('fornecedores: editar faz PUT e mostra erro de CNPJ duplicado', async () => {
    const api = simularApi({ 'GET /fornecedores': [fornecedor], 'PUT /fornecedores/3': erroHttp(409, 'CNPJ já cadastrado') })
    const { w } = await montar(FornecedoresView)
    await w.find('button[aria-label="Editar"]').trigger('click')
    expect(w.find('#razaoSocial').element.value).toBe('Litoral Alimentos')
    await w.find('#razaoSocial').setValue('x'.repeat(81))
    await botao(w, 'Salvar').trigger('click')
    expect(w.text()).toContain('Máximo de 80 caracteres')
    await w.find('#razaoSocial').setValue('Litoral SA')
    await botao(w, 'Salvar').trigger('click')
    await flushPromises()
    expect(api.ultima('PUT /fornecedores/3').corpo.razaoSocial).toBe('Litoral SA')
    expect(document.body.textContent).toContain('CNPJ já cadastrado')
  })

  it('CDs: lista capacidades próprias e inclui', async () => {
    const api = simularApi({ 'GET /cds': [{ id: 1, codigo: '806', nome: 'CD Rede Krill', capacidadeDiariaPaletes: null, ativo: true }, { id: 2, codigo: '807', nome: 'CD 2', capacidadeDiariaPaletes: 300, ativo: false }], 'POST /cds': { id: 3 } })
    const { w } = await montar(CentrosDistribuicaoView)
    expect(w.text()).toContain('300 pal.')
    expect(w.text()).toContain('Inativo')
    await botao(w, 'Novo CD').trigger('click')
    await w.find('#codigo').setValue('808')
    await w.find('#nome').setValue('CD Norte')
    w.findAllComponents(InputNumber).find((c) => c.props('inputId') === 'capacidadeDiariaPaletes').vm.$emit('update:modelValue', 200)
    await botao(w, 'Salvar').trigger('click')
    await flushPromises()
    expect(api.ultima('POST /cds').corpo).toMatchObject({ codigo: '808', nome: 'CD Norte', capacidadeDiariaPaletes: 200 })
  })

  it('produtos: filtro "somente sem peso"', async () => {
    const api = simularApi({ 'GET /produtos': (r) => (r.params.semPeso ? [] : [{ id: 1, codigo: 'ARZ5', descricao: 'Arroz', pesoBrutoKg: 5.2, ativo: true }, { id: 2, codigo: 'FEJ1', descricao: 'Feijão', pesoBrutoKg: null, ativo: true }]) })
    const { w } = await montar(ProdutosView)
    expect(w.text()).toContain('sem peso')
    expect(w.text()).toContain('5,2 kg')
    await botao(w, 'Somente sem peso').trigger('click')
    await new Promise((r) => setTimeout(r, 5))
    await flushPromises()
    expect(api.ultima('GET /produtos').params.semPeso).toBe('true')
  })

  it('tipos de caminhão, paletização e faixas usam o mesmo CRUD', async () => {
    simularApi({ 'GET /tipos-caminhao': [{ id: 1, nome: 'Carreta', capacidadePaletes: 28, ativo: true }], 'GET /tipos-paletizacao': [{ id: 2, nome: 'Não paletizado', fatorMultiplicador: 2, ativo: true }], 'GET /faixas-horarias': [{ id: 1, horaInicio: '08:00:00', horaFim: '10:00:00', capacidadePaletes: 120, ativo: true }] })
    expect((await montar(TiposCaminhaoView)).w.text()).toContain('Carreta')
    expect((await montar(TiposPaletizacaoView)).w.text()).toContain('Não paletizado')
    expect((await montar(FaixasView)).w.text()).toContain('08:00')
  })

  it('tempos de descarga: um parâmetro por tipo de caminhão', async () => {
    const api = simularApi({
      'GET /tipos-caminhao': [{ id: 1, nome: 'Carreta' }, { id: 2, nome: 'Van' }],
      'GET /parametros/descarga': [{ tipoCaminhaoId: 1, minutosPrevistos: 90, toleranciaEsperaMinutos: 30, custoPorPalete: 3.5 }],
      'PUT /parametros/descarga/:id': (r) => r.corpo
    })
    const { w } = await montar(ParametrosDescargaView)
    expect(w.text()).toContain('Carreta')
    expect(w.text()).toContain('Van')
    await w.findAll('button').filter((b) => b.text().includes('Salvar'))[1].trigger('click')
    await flushPromises()
    expect(api.ultima('PUT /parametros/descarga/2').corpo).toEqual({ minutosPrevistos: 60, toleranciaEsperaMinutos: 30, custoPorPalete: 0 })
  })
})
