package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.Conferente;

import java.util.List;
import java.util.Optional;

public interface ConferenteRepositorio {
    /** cdId nulo = todos os CDs. */
    List<Conferente> listar(Long cdId);

    Optional<Conferente> buscarPorId(Long id);

    boolean existeNome(Long cdId, String nome, Long ignorarId);

    Conferente salvar(Conferente conferente);
}
