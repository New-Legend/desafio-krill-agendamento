import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusAgendamento from '../src/components/StatusAgendamento.vue'

describe('StatusAgendamento', () => {
  it.each([
    ['AGENDADO', 'Agendado', 'agendado'], ['CONFIRMADO', 'Confirmado', 'confirmado'], ['CHEGOU', 'Chegou', 'chegou'],
    ['EM_RECEBIMENTO', 'Em recebimento', 'em_recebimento'], ['RECEBIDO', 'Recebido', 'recebido'], ['CANCELADO', 'Cancelado', 'cancelado'],
    ['NAO_COMPARECEU', 'Não compareceu', 'nao_compareceu'], ['OUTRO', 'OUTRO', 'outro']
  ])('%s usa a cor do Gestão CD', (status, rotulo, classe) => {
    const w = mount(StatusAgendamento, { props: { status } })
    expect(w.text()).toBe(rotulo)
    expect(w.classes()).toEqual(['status', classe])
  })
})
