package br.com.redekrill.agendamento.application.port;

/** Gera tokens secretos (link do fornecedor) e o hash guardado no banco. */
public interface GeradorToken {
    String novo();

    String hash(String token);
}
