package br.com.redekrill.agendamento.application.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record RegistroAuditoria(Long id, OffsetDateTime dataHora, String usuario, String acao, String entidade,
                                String entidadeId, String ip, boolean sucesso, Map<String, Object> detalhes) {
}
