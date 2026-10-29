package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.ConfiguracaoRepositorio;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConfiguracaoRepositorioJdbc implements ConfiguracaoRepositorio {
    private final JdbcClient db;

    public ConfiguracaoRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public Optional<String> obter(String chave) {
        return db.sql("select valor from configuracao where chave = ?").param(chave).query(String.class).optional();
    }

    @Override
    public void salvar(String chave, String valor) {
        db.sql("insert into configuracao (chave, valor) values (?, ?) on conflict (chave) do update set valor = excluded.valor")
                .params(chave, valor).update();
    }
}
