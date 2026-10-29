package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.NotificacaoEnvio;

import java.util.List;

public interface NotificacaoEnvioRepositorio {
    NotificacaoEnvio salvar(NotificacaoEnvio envio);

    List<NotificacaoEnvio> listar(String documentoTipo, Long documentoId);
}
