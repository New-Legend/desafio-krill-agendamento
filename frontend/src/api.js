import { sessao, encerrarSessao } from './sessao'

export class ApiError extends Error {
  constructor(status, mensagem, detalhes = []) {
    super(mensagem)
    this.status = status
    this.detalhes = detalhes
  }
}

let aoExpirar = () => {}
export function quandoSessaoExpirar(fn) {
  aoExpirar = fn
}

function query(params) {
  if (!params) return ''
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') q.append(k, v)
  })
  const s = q.toString()
  return s ? `?${s}` : ''
}

export async function api(caminho, { method = 'GET', body, params, blob = false } = {}) {
  const headers = { Accept: blob ? '*/*' : 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (sessao.atual?.token) headers.Authorization = `Bearer ${sessao.atual.token}`
  let resp
  try {
    resp = await fetch(`/api${caminho}${query(params)}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  } catch {
    throw new ApiError(0, 'Sem conexão com o servidor')
  }
  if (resp.status === 401 && caminho !== '/auth/login') {
    encerrarSessao()
    aoExpirar()
    throw new ApiError(401, 'Sessão expirada. Entre novamente.')
  }
  if (!resp.ok) {
    let corpo = {}
    try { corpo = await resp.json() } catch { /* sem corpo */ }
    const msg = corpo.mensagem || (resp.status === 403 ? 'Acesso negado para o seu perfil' : `Erro ${resp.status}`)
    throw new ApiError(resp.status, msg, corpo.detalhes || [])
  }
  if (resp.status === 204) return null
  if (blob) return resp.blob()
  return resp.json()
}
