import { afterEach, describe, expect, it } from 'vitest'
import { erroHttp, logar, montar, sair, simularApi, PERMISSOES } from '../montar'
import InicioView from '../../src/views/InicioView.vue'
import { menuItens } from '../../src/menu'

describe('InicioView (centrais do Gestão CD)', () => {
  afterEach(sair)

  it('admin vê as cinco centrais em acordeão', async () => {
    logar('ADMIN')
    simularApi({ 'GET /dashboard': erroHttp(500, 'fora') })
    const { w } = await montar(InicioView)
    const cards = w.findAll('.central-card')
    expect(cards.map((c) => c.find('.eyebrow').text())).toEqual(['OPE', 'REC', 'PAL', 'CAD', 'ADM'])
    expect(w.find('.central-links').exists()).toBe(false)
    await cards[1].find('button').trigger('click')
    expect(cards[1].classes()).toContain('aberto')
    expect(cards[1].text()).toContain('Rotina do dia')
    await cards[1].find('button').trigger('click')
    expect(w.find('.central-links').exists()).toBe(false)
  })

  it('usuário comum vê só operação, recebimento e paletes', () => {
    const consulta = (c) => PERMISSOES.USUARIO.includes(c)
    expect(menuItens(consulta).map((g) => g.grupo)).toEqual(['Operação', 'Recebimento', 'Paletes'])
    expect(menuItens(consulta)[0].itens.map((i) => i.to)).not.toContain('/agendamentos/novo')
    expect(menuItens(() => true)[0].itens.map((i) => i.to)).toContain('/agendamentos/novo')
    const portaria = (c) => [1102, 1204, 1205].includes(c)
    expect(menuItens(portaria).flatMap((g) => g.itens.map((i) => i.to))).toEqual(['/inicio', '/agendamentos', '/patio'])
  })
})
