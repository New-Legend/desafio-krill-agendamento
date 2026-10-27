package br.com.redekrill.agendamento.application.port;

import java.util.function.Supplier;

/** Abstração de transação para os casos de uso não dependerem do Spring. */
public interface Transacao {
    <T> T executar(Supplier<T> operacao);
}
