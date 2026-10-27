package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.domain.model.Agendamento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepositorio {
    Agendamento salvar(Agendamento agendamento);

    Optional<Agendamento> buscarPorId(Long id);

    List<Agendamento> listarPorPeriodo(LocalDate inicio, LocalDate fim);

    List<Agendamento> listar(FiltroAgendamento filtro);

    /** Serializa criações concorrentes no mesmo dia (lock transacional) para o cálculo de ocupação ser consistente. */
    void bloquearDia(LocalDate data);

    Optional<Agendamento> buscarPorTokenHash(String hash);
}
