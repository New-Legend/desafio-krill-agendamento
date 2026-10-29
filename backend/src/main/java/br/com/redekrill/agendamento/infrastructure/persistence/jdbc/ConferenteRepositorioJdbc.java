package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.ConferenteRepositorio;
import br.com.redekrill.agendamento.domain.model.Conferente;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ConferenteRepositorioJdbc implements ConferenteRepositorio {
    private static final RowMapper<Conferente> MAP = (r, i) -> new Conferente(r.getLong("id"), r.getLong("cd_id"),
            r.getString("nome"), r.getBoolean("ativo"));
    private final JdbcClient db;

    public ConferenteRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public List<Conferente> listar(Long cdId) {
        return db.sql("select * from conferente where (cast(? as bigint) is null or cd_id = ?) order by ativo desc, lower(nome)")
                .params(cdId, cdId).query(MAP).list();
    }

    @Override
    public Optional<Conferente> buscarPorId(Long id) {
        return db.sql("select * from conferente where id = ?").param(id).query(MAP).optional();
    }

    @Override
    public boolean existeNome(Long cdId, String nome, Long ignorarId) {
        return db.sql("select count(*) from conferente where cd_id = ? and lower(nome) = lower(?) and id <> coalesce(?, -1)")
                .params(cdId, nome, ignorarId).query(Long.class).single() > 0;
    }

    @Override
    public Conferente salvar(Conferente c) {
        Long id = c.id() == null
                ? db.sql("insert into conferente (cd_id, nome, ativo) values (?, ?, ?) returning id")
                .params(c.cdId(), c.nome(), c.ativo()).query(Long.class).single()
                : db.sql("update conferente set cd_id = ?, nome = ?, ativo = ? where id = ? returning id")
                .params(c.cdId(), c.nome(), c.ativo(), c.id()).query(Long.class).single();
        return buscarPorId(id).orElseThrow();
    }
}
