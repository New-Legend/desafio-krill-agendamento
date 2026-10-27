package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.CentroDistribuicao;

import java.util.List;
import java.util.Optional;

public interface CentroDistribuicaoRepositorio {
    List<CentroDistribuicao> listar();

    Optional<CentroDistribuicao> buscarPorId(Long id);

    boolean existeCodigo(String codigo, Long ignorarId);

    CentroDistribuicao salvar(CentroDistribuicao cd);
}
