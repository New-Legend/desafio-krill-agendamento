package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.CobrancaPalete;

import java.util.List;
import java.util.Optional;

public interface CobrancaPaleteRepositorio {
    CobrancaPalete salvar(CobrancaPalete cobranca);

    Optional<CobrancaPalete> buscarPorId(Long id);

    List<CobrancaPalete> listar(String fornecedor, CobrancaPalete.Status status);

    long proximoRecibo();
}
