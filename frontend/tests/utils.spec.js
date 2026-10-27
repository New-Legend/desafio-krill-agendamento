import { describe, expect, it } from 'vitest'
import { deIso, fmtData, fmtPct, isoData, nivelOcupacao, somarDias, validarAgendamento, diaSemana, fmtHora } from '../src/utils'

describe('utils', () => {
  it('converte datas sem fuso', () => {
    expect(isoData(new Date(2026, 10, 4))).toBe('2026-11-04')
    expect(isoData('2026-11-04T10:00')).toBe('2026-11-04')
    expect(isoData(null)).toBeNull()
    expect(deIso('2026-11-04').getDate()).toBe(4)
    expect(deIso(null)).toBeNull()
    expect(somarDias('2026-10-31', 1)).toBe('2026-11-01')
    expect(fmtData('2026-11-04')).toBe('04/11/2026')
    expect(fmtData(null)).toBe('')
    expect(diaSemana('2026-11-04')).toBe('qua')
    expect(fmtHora('08:00:00')).toBe('08:00')
  })

  it('classifica ocupação', () => {
    expect(nivelOcupacao(50)).toBe('ok')
    expect(nivelOcupacao(80)).toBe('atencao')
    expect(nivelOcupacao(100)).toBe('atencao')
    expect(nivelOcupacao(116.7)).toBe('excedido')
    expect(fmtPct(116.7)).toBe('116,7%')
  })

  it('valida agendamento como o backend', () => {
    const ok = { data: '2026-11-04', faixaHorariaId: 1, idPedido: 10, fornecedor: 'F', emailFornecedor: 'a@b.com', tipoCaminhaoId: 1, tipoPaletizacaoId: 1, quantidadePaletes: 2 }
    expect(validarAgendamento(ok)).toEqual({})
    const e = validarAgendamento({ ...ok, emailFornecedor: 'invalido', fornecedor: 'x'.repeat(81), quantidadePaletes: 0, data: null })
    expect(Object.keys(e).sort()).toEqual(['data', 'emailFornecedor', 'fornecedor', 'quantidadePaletes'])
    expect(Object.keys(validarAgendamento({})).length).toBe(8)
  })
})
