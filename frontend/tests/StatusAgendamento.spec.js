import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusAgendamento from '../src/components/StatusAgendamento.vue'

const Tag = { props: ['value', 'severity'], template: '<span :data-sev="severity">{{ value }}</span>' }

describe('StatusAgendamento', () => {
  it.each([
    ['AGENDADO', 'Agendado', 'info'], ['CONFIRMADO', 'Confirmado', 'success'], ['CHEGOU', 'Chegou', 'warn'],
    ['EM_RECEBIMENTO', 'Em recebimento', 'warn'], ['RECEBIDO', 'Recebido', 'success'], ['CANCELADO', 'Cancelado', 'danger'],
    ['NAO_COMPARECEU', 'Não compareceu', 'secondary'], ['OUTRO', 'OUTRO', 'secondary']
  ])('%s', (status, rotulo, sev) => {
    const w = mount(StatusAgendamento, { props: { status }, global: { stubs: { Tag } } })
    expect(w.text()).toBe(rotulo)
    expect(w.find('span').attributes('data-sev')).toBe(sev)
  })
})
