import { computed, ref } from 'vue'
import { api } from '../api'

/** Tipos de palete configuráveis (Cadastros → Tipos de palete). Cache simples por sessão de tela. */
const tipos = ref([])
let carregando = null

export function useTiposPalete() {
  async function carregar(forcar = false) {
    if (!forcar && tipos.value.length) return tipos.value
    carregando ??= api('/tipos-palete').then((t) => (tipos.value = t || [])).finally(() => (carregando = null))
    return carregando
  }
  const ativos = computed(() => tipos.value.filter((t) => t.ativo))
  const rotulo = (codigo) => tipos.value.find((t) => t.codigo === codigo)?.nome || codigo
  /** Tipos a exibir para um conjunto de quantidades: ativos + os que aparecem nos dados (mesmo inativos). */
  function colunas(...mapas) {
    const usados = new Set(mapas.flatMap((m) => Object.keys(m || {})))
    const lista = tipos.value.filter((t) => t.ativo || usados.has(t.codigo)).map((t) => t.codigo)
    usados.forEach((c) => !lista.includes(c) && lista.push(c))
    return lista
  }
  return { tipos, ativos, carregar, rotulo, colunas }
}

export const zerar = (codigos) => Object.fromEntries(codigos.map((c) => [c, 0]))
export const somar = (q) => Object.values(q || {}).reduce((s, n) => s + (n || 0), 0)
