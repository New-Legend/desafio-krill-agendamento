import { reactive, computed } from 'vue'

const CHAVE = 'agendamento.sessao'

function carregar() {
  try {
    const s = JSON.parse(localStorage.getItem(CHAVE) || 'null')
    if (s && s.expiraEm > Date.now()) return s
  } catch { /* sessão inválida */ }
  localStorage.removeItem(CHAVE)
  return null
}

export const sessao = reactive({ atual: carregar() })

export const isAdmin = computed(() => sessao.atual?.perfil === 'ADMIN')
export const logado = computed(() => !!sessao.atual && sessao.atual.expiraEm > Date.now())

export function iniciarSessao(resp) {
  sessao.atual = {
    token: resp.token,
    usuario: resp.usuario,
    nome: resp.nome,
    perfil: resp.perfil,
    expiraEm: Date.now() + resp.expiraEmSegundos * 1000
  }
  localStorage.setItem(CHAVE, JSON.stringify(sessao.atual))
}

export function encerrarSessao() {
  sessao.atual = null
  localStorage.removeItem(CHAVE)
}
