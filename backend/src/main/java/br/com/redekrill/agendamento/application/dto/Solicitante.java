package br.com.redekrill.agendamento.application.dto;

/** Quem executa a ação e de onde (usado em auditoria). */
public record Solicitante(String usuario, String ip) {
}
