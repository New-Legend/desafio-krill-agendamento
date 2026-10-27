package br.com.redekrill.agendamento.application.dto;

import br.com.redekrill.agendamento.domain.model.StatusAgendamento;

import java.time.LocalDate;

public record FiltroAgendamento(LocalDate inicio, LocalDate fim, StatusAgendamento status, String fornecedor,
                                Long idPedido, Long faixaHorariaId) {
}
