package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.CapacidadeRecebimento;

import java.util.List;
import java.util.Optional;

public interface CapacidadeRecebimentoRepositorio {
    List<CapacidadeRecebimento> listar();

    Optional<CapacidadeRecebimento> buscarPorId(Long id);

    CapacidadeRecebimento salvar(CapacidadeRecebimento c);

    void excluir(Long id);
}
