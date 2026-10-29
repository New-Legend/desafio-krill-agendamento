package br.com.redekrill.agendamento.domain.model;

import java.time.OffsetDateTime;

/**
 * Dados logísticos do agendamento: entrega direta na loja (nulo = no CD), transportadora,
 * doca atribuída no pátio e modal de descarga informado (nulo = deduzido do tipo de caminhão).
 */
public record LogisticaAgendamento(Long agendamentoId, String loja, String transportadora, Long docaId,
                                   OffsetDateTime docaEm, String modalDescarga) {
    public LogisticaAgendamento {
        loja = Textos.opcional(loja, "Loja", 80);
        transportadora = Textos.opcional(transportadora, "Transportadora", 80);
        modalDescarga = modalDescarga == null || modalDescarga.isBlank() ? null : CalculadoraTaxaDescarga.codigo(modalDescarga);
    }

    public static LogisticaAgendamento vazia(Long agendamentoId) {
        return new LogisticaAgendamento(agendamentoId, null, null, null, null, null);
    }

    public LogisticaAgendamento comDoca(Long doca, OffsetDateTime em) {
        return new LogisticaAgendamento(agendamentoId, loja, transportadora, doca, doca == null ? null : em, modalDescarga);
    }

    public LogisticaAgendamento comDados(String novaLoja, String novaTransportadora, String modal) {
        return new LogisticaAgendamento(agendamentoId, novaLoja, novaTransportadora, docaId, docaEm, modal);
    }

    public boolean entregaNaLoja() {
        return loja != null;
    }
}
