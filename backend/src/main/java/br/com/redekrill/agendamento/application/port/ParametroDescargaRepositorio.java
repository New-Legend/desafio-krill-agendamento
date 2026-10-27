package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.ParametroDescarga;

import java.util.List;

public interface ParametroDescargaRepositorio {
    List<ParametroDescarga> listar();

    ParametroDescarga salvar(ParametroDescarga parametro);
}
