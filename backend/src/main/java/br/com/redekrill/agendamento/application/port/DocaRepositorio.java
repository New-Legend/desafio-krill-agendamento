package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.Doca;

import java.util.List;
import java.util.Optional;

public interface DocaRepositorio {
    /** cdId nulo = todos os CDs. */
    List<Doca> listar(Long cdId);

    Optional<Doca> buscarPorId(Long id);

    boolean existeCodigo(Long cdId, String codigo, Long ignorarId);

    Doca salvar(Doca doca);
}
