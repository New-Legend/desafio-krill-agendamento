package br.com.redekrill.agendamento.application.dto;

import br.com.redekrill.agendamento.domain.model.AlertaCapacidade;
import br.com.redekrill.agendamento.domain.model.OcupacaoDia;

import java.util.List;

public record Simulacao(OcupacaoDia ocupacaoAtual, int paletesEquivalentes, List<AlertaCapacidade> alertas) {
}
