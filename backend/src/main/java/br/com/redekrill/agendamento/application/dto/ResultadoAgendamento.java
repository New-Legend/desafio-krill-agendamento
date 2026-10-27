package br.com.redekrill.agendamento.application.dto;

import br.com.redekrill.agendamento.domain.model.AlertaCapacidade;

import java.util.List;

public record ResultadoAgendamento(AgendamentoDetalhe agendamento, List<AlertaCapacidade> alertas, String linkFornecedor) {
}
