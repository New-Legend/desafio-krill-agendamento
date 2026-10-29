package br.com.redekrill.agendamento.application.port;

import java.util.Optional;

/** Configurações chave/valor do sistema (tabela configuracao). */
public interface ConfiguracaoRepositorio {
    Optional<String> obter(String chave);

    void salvar(String chave, String valor);
}
