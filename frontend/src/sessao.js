import { reactive, computed } from 'vue'

const CHAVE = 'agendamento.sessao'

function carregar() {
  try {
    const s = JSON.parse(localStorage.getItem(CHAVE) || 'null')
    if (s && s.expiraEm > Date.now() && Array.isArray(s.permissoes)) return s
  } catch { /* sessão inválida */ }
  localStorage.removeItem(CHAVE)
  return null
}

export const sessao = reactive({ atual: carregar() })

export const logado = computed(() => !!sessao.atual && sessao.atual.expiraEm > Date.now())

/** Permissão numerada do perfil (ex.: 1103 = incluir agendamento). O backend valida de novo em cada chamada. */
export function pode(codigo) {
  return !!sessao.atual?.permissoes?.includes(codigo)
}

export function iniciarSessao(resp) {
  sessao.atual = {
    token: resp.token,
    usuario: resp.usuario,
    nome: resp.nome,
    perfil: resp.perfil,
    permissoes: resp.permissoes || [],
    trocarSenha: !!resp.trocarSenha,
    expiraEm: Date.now() + resp.expiraEmSegundos * 1000
  }
  salvar()
}

/** Atualiza perfil/permissões com /auth/eu (perfil pode ter mudado desde o login). */
export function atualizarSessao(eu) {
  if (!sessao.atual) return
  Object.assign(sessao.atual, { nome: eu.nome, perfil: eu.perfil, permissoes: eu.permissoes, trocarSenha: eu.trocarSenha })
  salvar()
}

function salvar() {
  localStorage.setItem(CHAVE, JSON.stringify(sessao.atual))
}

export function encerrarSessao() {
  sessao.atual = null
  localStorage.removeItem(CHAVE)
}
