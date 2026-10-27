package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;

public interface NotificadorEmail {
    void enviarConfirmacao(AgendamentoDetalhe agendamento);

    void enviarCancelamento(AgendamentoDetalhe agendamento);
}
