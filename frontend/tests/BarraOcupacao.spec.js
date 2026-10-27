import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BarraOcupacao from '../src/components/BarraOcupacao.vue'

describe('BarraOcupacao', () => {
  it('limita a barra em 100% e marca excedido', () => {
    const w = mount(BarraOcupacao, { props: { percentual: 116.7, ocupado: 140, capacidade: 120 } })
    expect(w.find('.barra').classes()).toContain('excedido')
    expect(w.find('.barra span').attributes('style')).toContain('width: 100%')
    expect(w.text()).toContain('140 / 120 paletes')
  })
  it('esconde texto quando pedido', () => {
    const w = mount(BarraOcupacao, { props: { percentual: 40, mostrarTexto: false } })
    expect(w.find('.ocupacao-inline').exists()).toBe(false)
    expect(w.find('.barra').classes()).toContain('ok')
  })
})
