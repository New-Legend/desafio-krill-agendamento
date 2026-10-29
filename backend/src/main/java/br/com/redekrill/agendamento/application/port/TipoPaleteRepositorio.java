package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.TipoPalete;

import java.util.List;
import java.util.Optional;

public interface TipoPaleteRepositorio {
    List<TipoPalete> listar();

    Optional<TipoPalete> buscarPorId(Long id);

    boolean existeCodigo(String codigo, Long ignorarId);

    /** Já usado em algum vale ou romaneio (não pode ser excluído, só inativado). */
    boolean emUso(String codigo);

    TipoPalete salvar(TipoPalete tipo);

    void excluir(Long id);
}
