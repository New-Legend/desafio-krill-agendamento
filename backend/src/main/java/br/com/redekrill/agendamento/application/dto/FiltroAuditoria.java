package br.com.redekrill.agendamento.application.dto;

import java.time.LocalDate;

public record FiltroAuditoria(LocalDate inicio, LocalDate fim, String usuario, String acao, String entidade) {
}
