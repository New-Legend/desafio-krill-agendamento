package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.Produto;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepositorio {
    List<Produto> listar(String busca, Boolean semPeso);

    Optional<Produto> buscarPorId(Long id);

    Optional<Produto> buscarPorCodigo(String codigo);

    Optional<Produto> buscarPorEan(String ean);

    Produto salvar(Produto produto);
}
