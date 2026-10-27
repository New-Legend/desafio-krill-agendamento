package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;

public interface NotificadorEmail {
    /** linkFornecedor: página pública para o fornecedor acompanhar/confirmar. */
    void enviarConfirmacao(AgendamentoDetalhe agendamento, String linkFornecedor);

    void enviarCancelamento(AgendamentoDetalhe agendamento);
}
