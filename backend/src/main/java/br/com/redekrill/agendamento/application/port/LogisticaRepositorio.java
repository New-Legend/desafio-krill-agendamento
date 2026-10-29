package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.LogisticaAgendamento;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Dados logísticos dos agendamentos (loja, transportadora, doca, modal) e consultas do pátio. */
public interface LogisticaRepositorio {
    Optional<LogisticaAgendamento> buscar(Long agendamentoId);

    Map<Long, LogisticaAgendamento> porAgendamentos(Collection<Long> ids);

    LogisticaAgendamento salvar(LogisticaAgendamento logistica);

    /** Agendamento que ocupa a doca agora (chegou ou em recebimento), ignorando o informado. */
    Optional<Long> ocupanteDaDoca(Long docaId, Long ignorarAgendamentoId);

    /** Paletes equivalentes e caminhões agendados no dia (que ocupam capacidade) para o CD (sem loja) ou para a loja. */
    Consumo consumo(LocalDate data, Long cdId, String loja);

    /** Saldo de vales-palete agrupado pela transportadora do agendamento (ou fornecedor, quando não informada). */
    List<SaldoTransportadora> saldosPorTransportadora();

    record Consumo(int paletes, int caminhoes) {
    }

    record SaldoTransportadora(String transportadora, int vales, int entregues, int devolvidos, int saldo) {
    }
}
