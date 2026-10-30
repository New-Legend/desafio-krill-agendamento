import { afterEach, describe, expect, it } from 'vitest'
import { flushPromises, logar, montar, sair, simularApi } from '../montar'
import { guarda, routes } from '../../src/router'
import { sessao } from '../../src/sessao'
import UsuariosView from '../../src/views/UsuariosView.vue'
import PerfisView from '../../src/views/PerfisView.vue'
import LogsAcessoView from '../../src/views/LogsAcessoView.vue'
import TrocarSenhaView from '../../src/views/TrocarSenhaView.vue'
import BotaoPermissoes from '../../src/components/BotaoPermissoes.vue'

const CATALOGO = [
  { codigo: 1001, modulo: 'Administração', nome: 'Usuários: consultar', telas: ['usuarios'] },
  { codigo: 1002, modulo: 'Administração', nome: 'Usuários: incluir, alterar e redefinir senha', telas: ['usuarios'] },
  { codigo: 1003, modulo: 'Administração', nome: 'Perfis e permissões', telas: ['perfis', 'usuarios'] },
  { codigo: 1102, modulo: 'Operação', nome: 'Agendamentos: consultar', telas: ['agendamentos'] },
  { codigo: 1103, modulo: 'Operação', nome: 'Agendamentos: incluir', telas: ['novo', 'agendamentos'] }
]
const PERFIS = [
  { id: 1, nome: 'Administrador', descricao: 'Todas as telas', ativo: true, permissoes: [1001, 1002, 1003, 1102, 1103], usuarios: 1 },
  { id: 3, nome: 'Portaria', descricao: 'Guarita', ativo: true, permissoes: [1102], usuarios: 1 }
]
const USUARIOS = [
  { id: 1, login: 'admin', nome: 'Administrador', perfilId: 1, perfil: 'Administrador', ativo: true, trocarSenha: false, ultimoAcesso: '2026-10-30T08:10:00Z' },
  { id: 2, login: 'carlos.portaria', nome: 'Carlos Mendes', perfilId: 3, perfil: 'Portaria', ativo: true, trocarSenha: true, ultimoAcesso: null },
  { id: 3, login: 'ana.lima', nome: 'Ana Lima', perfilId: 3, perfil: 'Portaria', ativo: false, trocarSenha: false, ultimoAcesso: null }
]
const rota = (path) => {
  const r = routes.find((x) => x.path === path)
  return { name: r.name, meta: r.meta || {}, fullPath: path }
}

describe('gestão de acessos', () => {
  afterEach(sair)

  it('usuários: filtra ativos, mostra pílulas e último acesso', async () => {
    logar('ADMIN', { usuario: 'admin' })
    simularApi({ 'GET /usuarios': USUARIOS, 'GET /perfis': PERFIS, 'GET /auth/permissoes': CATALOGO })
    const { w } = await montar(UsuariosView)
    const texto = w.find('.tabela-acesso').text()
    expect(texto).toContain('Carlos Mendes')
    expect(texto).toContain('Troca de senha pendente')
    expect(texto).toContain('Nunca entrou')
    expect(texto).not.toContain('Ana Lima')
    expect(w.findAll('.cartao-acesso')).toHaveLength(2)
    await w.find('#uf-texto').setValue('carlos')
    expect(w.findAll('.cartao-acesso')).toHaveLength(1)
  })

  it('usuários: valida e inclui com senha provisória', async () => {
    logar('ADMIN', { usuario: 'admin' })
    const api = simularApi({ 'GET /usuarios': USUARIOS, 'GET /perfis': PERFIS, 'POST /usuarios': { id: 9 } })
    const { w, toast } = await montar(UsuariosView)
    await w.findAll('button').find((b) => b.text() === 'Novo usuário').trigger('click')
    await flushPromises()
    const salvar = () => [...document.querySelectorAll('button')].find((b) => b.textContent.includes('Salvar')).click()
    salvar()
    await flushPromises()
    expect(document.body.textContent).toContain('Informe o nome')
    expect(document.body.textContent).toContain('Escolha o perfil')
    document.querySelector('#u-nome').value = 'Joana Prado'
    document.querySelector('#u-nome').dispatchEvent(new Event('input'))
    document.querySelector('#u-login').value = 'Joana.Prado'
    document.querySelector('#u-login').dispatchEvent(new Event('input'))
    document.querySelector('#u-senha').value = 'semnumero'
    document.querySelector('#u-senha').dispatchEvent(new Event('input'))
    w.findComponent(UsuariosView).vm.$.setupState.form.perfilId = 3
    salvar()
    await flushPromises()
    expect(document.body.textContent).toContain('Use letras e números')
    document.querySelector('#u-senha').value = 'Guarita2026'
    document.querySelector('#u-senha').dispatchEvent(new Event('input'))
    salvar()
    await flushPromises()
    expect(api.ultima('POST /usuarios').corpo).toEqual({ login: 'joana.prado', nome: 'Joana Prado', perfilId: 3, ativo: true, senhaProvisoria: 'Guarita2026' })
    expect(toast()).toContain('primeiro acesso')
  })

  it('usuários: menu ⋮ redefine senha e inativa', async () => {
    logar('ADMIN', { usuario: 'admin' })
    const api = simularApi({ 'GET /usuarios': USUARIOS, 'GET /perfis': PERFIS, 'POST /usuarios/2/senha': null, 'PUT /usuarios/2': {} })
    const { w } = await montar(UsuariosView)
    const st = w.findComponent(UsuariosView).vm.$.setupState
    st.menuAlvo = USUARIOS[1]
    const itens = st.itensMenu
    expect(itens.map((i) => i.label)).toEqual(['Editar', 'Redefinir senha', 'Inativar'])
    itens[1].command()
    await flushPromises()
    st.senha.valor = '123'
    st.redefinir()
    expect(st.senha.erro).toBe('Mínimo de 8 caracteres')
    st.senha.valor = 'Nova2026x'
    st.redefinir()
    await flushPromises()
    expect(api.ultima('POST /usuarios/2/senha').corpo).toEqual({ senhaProvisoria: 'Nova2026x' })
    itens[2].command()
    await flushPromises()
    expect(api.ultima('PUT /usuarios/2').corpo).toEqual({ nome: 'Carlos Mendes', perfilId: 3, ativo: false })
    st.menuAlvo = USUARIOS[0]
    expect(st.itensMenu[2].disabled).toBe(true)
  })

  it('perfis: marca módulo inteiro e salva os números', async () => {
    logar('ADMIN')
    const api = simularApi({ 'GET /perfis': PERFIS, 'GET /auth/permissoes': CATALOGO, 'PUT /perfis/3': { ...PERFIS[1], permissoes: [1102, 1103] }, 'GET /auth/eu': { nome: 'A', perfil: 'Administrador', permissoes: [1003], trocarSenha: false } })
    const { w } = await montar(PerfisView, { rota: '/acessos/perfis?perfil=Portaria' })
    expect(w.find('.lista-perfis .selecionado').text()).toContain('Portaria')
    expect(w.findAll('.modulo-permissoes')).toHaveLength(2)
    const operacao = w.findAll('.modulo-permissoes')[1]
    expect(operacao.text()).toContain('1103')
    await operacao.find('button').trigger('click')
    await w.findAll('button').find((b) => b.text() === 'Salvar perfil').trigger('click')
    await flushPromises()
    expect(api.ultima('PUT /perfis/3').corpo).toEqual({ nome: 'Portaria', descricao: 'Guarita', ativo: true, permissoes: [1102, 1103] })
    await w.findAll('button').find((b) => b.text() === 'Novo perfil').trigger('click')
    await w.findAll('button').find((b) => b.text() === 'Incluir perfil').trigger('click')
    expect(w.text()).toContain('ao menos 3 caracteres')
  })

  it('logs de acesso: filtra por evento e mostra navegador', async () => {
    logar('ADMIN')
    const api = simularApi({ 'GET /logs-acesso': [{ id: 1, usuario: 'admin', evento: 'SENHA_RECUSADA', rotulo: 'Senha recusada', ip: '10.0.0.5', navegador: 'Chrome · Windows', criadoEm: '2026-10-30T08:00:00Z' }] })
    const { w } = await montar(LogsAcessoView)
    expect(w.text()).toContain('Senha recusada')
    expect(w.text()).toContain('Chrome · Windows')
    w.findComponent(LogsAcessoView).vm.$.setupState.filtro.evento = 'SAIDA'
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(api.ultima('GET /logs-acesso').params).toMatchObject({ evento: 'SAIDA', dias: '7', limite: '200' })
  })

  it('troca de senha obrigatória: guarda redireciona e a tela valida', async () => {
    logar('USUARIO', { trocarSenha: true })
    expect(guarda(rota('/agendamentos'))).toEqual({ name: 'trocar-senha' })
    expect(guarda(rota('/trocar-senha'))).toBe(true)
    expect(guarda(rota('/acessos/usuarios'))).toEqual({ name: 'trocar-senha' })
    const api = simularApi({ 'POST /auth/senha': null, 'GET /auth/eu': { nome: 'U', perfil: 'Consulta', permissoes: [1102], trocarSenha: false } })
    const { w, router } = await montar(TrocarSenhaView)
    await w.find('form').trigger('submit')
    expect(w.text()).toContain('Informe a senha atual')
    await w.find('#ts-atual').setValue('Provisoria1')
    await w.find('#ts-nova').setValue('NovaSenha9')
    await w.find('#ts-conf').setValue('NovaSenha8')
    await w.find('form').trigger('submit')
    expect(w.text()).toContain('As senhas não conferem')
    await w.find('#ts-conf').setValue('NovaSenha9')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(api.ultima('POST /auth/senha').corpo).toEqual({ senhaAtual: 'Provisoria1', novaSenha: 'NovaSenha9' })
    expect(sessao.atual.trocarSenha).toBe(false)
    expect(router.currentRoute.value.path).toBe('/inicio')
    expect(guarda(rota('/acessos/usuarios'))).toEqual({ name: 'inicio' })
  })

  it('botão Permissões mostra os números da tela e o que o perfil tem', async () => {
    logar('USUARIO')
    simularApi({ 'GET /auth/permissoes': CATALOGO })
    const { w } = await montar(BotaoPermissoes, { props: { tela: 'agendamentos' } })
    await w.find('.botao-permissoes').trigger('click')
    await flushPromises()
    const itens = [...document.querySelectorAll('.lista-permissoes li')].map((li) => li.textContent)
    expect(itens[0]).toContain('1102')
    expect(itens[0]).toContain('Liberada')
    expect(itens[1]).toContain('1103')
    expect(itens[1]).toContain('Sem acesso')
    expect(document.body.textContent).not.toContain('Editar perfis')
    const vazio = await montar(BotaoPermissoes, { props: { tela: 'inicio' } })
    expect(vazio.w.find('.botao-permissoes').exists()).toBe(false)
  })
})
