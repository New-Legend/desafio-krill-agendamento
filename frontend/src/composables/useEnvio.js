import { ref } from 'vue'

/** Trava contra duplo clique: ignora novas chamadas enquanto a anterior não termina. */
export function useEnvio() {
  const enviando = ref(false)
  async function enviar(fn) {
    if (enviando.value) return undefined
    enviando.value = true
    try {
      return await fn()
    } finally {
      enviando.value = false
    }
  }
  return { enviando, enviar }
}
