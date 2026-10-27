package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.TipoCaminhao;

import java.util.List;
import java.util.Optional;

public interface TipoCaminhaoRepositorio {
    List<TipoCaminhao> listar();

    Optional<TipoCaminhao> buscarPorId(Long id);

    boolean existeNome(String nome, Long ignorarId);

    TipoCaminhao salvar(TipoCaminhao tipo);
}
