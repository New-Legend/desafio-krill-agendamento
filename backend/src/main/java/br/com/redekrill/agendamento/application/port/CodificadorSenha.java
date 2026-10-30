package br.com.redekrill.agendamento.application.port;

public interface CodificadorSenha {
    String codificar(String senha);

    boolean confere(String senha, String hash);
}
