package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.EventoAcesso;
import br.com.redekrill.agendamento.domain.model.LogAcesso;

import java.time.Instant;
import java.util.List;

public interface LogAcessoRepositorio {
    void registrar(String usuario, Long usuarioId, EventoAcesso evento, String ip, String navegador);

    List<LogAcesso> buscar(String texto, EventoAcesso evento, Instant desde, int limite);
}
