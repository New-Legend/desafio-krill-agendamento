import { ref } from 'vue'
import { api } from '../api'

const catalogo = ref([])
let carga = null

/** Catálogo de permissões numeradas (GET /auth/permissoes), carregado uma vez por sessão do navegador. */
export function usePermissoes() {
  if (!carga) {
    carga = api('/auth/permissoes')
      .then((lista) => { catalogo.value = lista })
      .catch(() => { carga = null })
  }
  return { catalogo, carregado: () => carga }
}

/** Agrupa por módulo mantendo a ordem numérica (10xx, 11xx...). */
export function porModulo(lista) {
  const grupos = []
  lista.forEach((p) => {
    let g = grupos.find((x) => x.modulo === p.modulo)
    if (!g) grupos.push((g = { modulo: p.modulo, itens: [] }))
    g.itens.push(p)
  })
  return grupos
}
