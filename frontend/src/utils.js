/** Data local no formato ISO (yyyy-mm-dd), sem conversão para UTC. */
export function isoData(d) {
  if (!d) return null
  if (typeof d === 'string') return d.slice(0, 10)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${dia}`
}

export function deIso(s) {
  if (!s) return null
  const [a, m, d] = s.slice(0, 10).split('-').map(Number)
  return new Date(a, m - 1, d)
}

export function hoje() {
  return isoData(new Date())
}

export function somarDias(iso, n) {
  const d = deIso(iso)
  d.setDate(d.getDate() + n)
  return isoData(d)
}

export function fmtData(iso) {
  if (!iso) return ''
  const [a, m, d] = iso.slice(0, 10).split('-')
  return `${d}/${m}/${a}`
}

const DIAS = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb']
export function diaSemana(iso) {
  return DIAS[deIso(iso).getDay()]
}

export function fmtDataHora(s) {
  if (!s) return ''
  return new Date(s).toLocaleString('pt-BR', {
    timeZone: 'America/Sao_Paulo', day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}

export function fmtHora(t) {
  return t ? t.slice(0, 5) : ''
}

export function fmtPct(v) {
  return `${Number(v || 0).toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 1 })}%`
}

/** Nível visual da ocupação: ok < 80% <= atencao <= 100% < excedido. */
export function nivelOcupacao(pct) {
  if (pct > 100) return 'excedido'
  if (pct >= 80) return 'atencao'
  return 'ok'
}

export const EMAIL_RE = /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/

/** Validação do formulário de agendamento (mesmas regras do backend). */
export function validarAgendamento(f) {
  const e = {}
  if (!f.data) e.data = 'Informe a data'
  if (!f.faixaHorariaId) e.faixaHorariaId = 'Selecione a faixa horária'
  if (!f.idPedido || f.idPedido < 1) e.idPedido = 'Informe o ID do pedido'
  if (!f.fornecedor?.trim()) e.fornecedor = 'Informe o fornecedor'
  else if (f.fornecedor.length > 80) e.fornecedor = 'Máximo de 80 caracteres'
  if (!f.emailFornecedor?.trim()) e.emailFornecedor = 'Informe o e-mail'
  else if (f.emailFornecedor.length > 80) e.emailFornecedor = 'Máximo de 80 caracteres'
  else if (!EMAIL_RE.test(f.emailFornecedor.trim())) e.emailFornecedor = 'E-mail inválido'
  if (!f.tipoCaminhaoId) e.tipoCaminhaoId = 'Selecione o tipo de caminhão'
  if (!f.tipoPaletizacaoId) e.tipoPaletizacaoId = 'Selecione o tipo de paletização'
  if (!f.quantidadePaletes || f.quantidadePaletes < 1) e.quantidadePaletes = 'Informe a quantidade de paletes'
  return e
}

export function baixarArquivo(blob, nome) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nome
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
