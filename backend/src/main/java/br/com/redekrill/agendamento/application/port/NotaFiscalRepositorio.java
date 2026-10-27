package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.NotaFiscal;

import java.util.List;
import java.util.Optional;

public interface NotaFiscalRepositorio {
    NotaFiscal salvar(NotaFiscal nota);

    Optional<NotaFiscal> buscarPorId(Long id);

    boolean existeChave(String chave);

    List<NotaFiscal> listarPorAgendamento(Long agendamentoId);

    List<NotaFiscal> listarPorAgendamentos(List<Long> agendamentoIds);

    void excluir(Long id);
}
