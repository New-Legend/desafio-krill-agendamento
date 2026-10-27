package br.com.redekrill.agendamento.application.dto;

import br.com.redekrill.agendamento.domain.model.Agendamento;

/** Agendamento com as descrições dos cadastros relacionados, pronto para exibição/e-mail. */
public record AgendamentoDetalhe(Agendamento agendamento, String faixaDescricao, String tipoCaminhao,
                                 String tipoPaletizacao) {
}
