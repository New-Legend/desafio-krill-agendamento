package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.FaixaHoraria;

import java.util.List;
import java.util.Optional;

public interface FaixaHorariaRepositorio {
    List<FaixaHoraria> listar();

    Optional<FaixaHoraria> buscarPorId(Long id);

    FaixaHoraria salvar(FaixaHoraria faixa);
}
