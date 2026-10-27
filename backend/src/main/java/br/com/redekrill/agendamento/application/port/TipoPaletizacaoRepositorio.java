package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;

import java.util.List;
import java.util.Optional;

public interface TipoPaletizacaoRepositorio {
    List<TipoPaletizacao> listar();

    Optional<TipoPaletizacao> buscarPorId(Long id);

    boolean existeNome(String nome, Long ignorarId);

    TipoPaletizacao salvar(TipoPaletizacao tipo);
}
