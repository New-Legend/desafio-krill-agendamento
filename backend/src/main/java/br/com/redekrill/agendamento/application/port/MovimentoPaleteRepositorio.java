package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.MovimentoPalete;

import java.time.LocalDate;
import java.util.List;

public interface MovimentoPaleteRepositorio {
    MovimentoPalete salvar(MovimentoPalete movimento);

    List<MovimentoPalete> listar(String loja, LocalDate inicio, LocalDate fim);

    long proximoRomaneio();
}
