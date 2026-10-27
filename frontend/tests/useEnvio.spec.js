import { describe, expect, it } from 'vitest'
import { useEnvio } from '../src/composables/useEnvio'

describe('useEnvio', () => {
  it('ignora duplo clique enquanto envia', async () => {
    const { enviando, enviar } = useEnvio()
    let chamadas = 0
    let liberar
    const fn = () => { chamadas++; return new Promise((r) => { liberar = r }) }
    const p1 = enviar(fn)
    const p2 = enviar(fn)
    expect(enviando.value).toBe(true)
    expect(await p2).toBeUndefined()
    liberar('ok')
    expect(await p1).toBe('ok')
    expect(chamadas).toBe(1)
    expect(enviando.value).toBe(false)
  })
})
