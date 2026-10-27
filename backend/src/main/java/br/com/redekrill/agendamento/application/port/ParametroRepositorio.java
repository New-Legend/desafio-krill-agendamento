package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.ParametroCapacidade;

public interface ParametroRepositorio {
    ParametroCapacidade obter();

    ParametroCapacidade salvar(ParametroCapacidade parametro);
}
