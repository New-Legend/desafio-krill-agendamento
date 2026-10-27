package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.Fornecedor;

import java.util.List;
import java.util.Optional;

public interface FornecedorRepositorio {
    List<Fornecedor> listar(String busca, Boolean ativo);

    Optional<Fornecedor> buscarPorId(Long id);

    Optional<Fornecedor> buscarPorCnpj(String cnpj);

    Fornecedor salvar(Fornecedor fornecedor);
}
