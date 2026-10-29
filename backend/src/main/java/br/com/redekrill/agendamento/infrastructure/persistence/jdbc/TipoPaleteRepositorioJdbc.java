package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.TipoPaleteRepositorio;
import br.com.redekrill.agendamento.domain.model.TipoPalete;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TipoPaleteRepositorioJdbc implements TipoPaleteRepositorio {
    private static final RowMapper<TipoPalete> MAP = (r, i) -> new TipoPalete(r.getLong("id"), r.getString("codigo"),
            r.getString("nome"), r.getBigDecimal("valor_reposicao"), r.getInt("ordem"), r.getBoolean("ativo"),
            r.getObject("cd_id", Long.class));
    private final JdbcClient db;

    public TipoPaleteRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public List<TipoPalete> listar() {
        return db.sql("select * from tipo_palete order by ordem, nome").query(MAP).list();
    }

    @Override
    public Optional<TipoPalete> buscarPorId(Long id) {
        return db.sql("select * from tipo_palete where id = ?").param(id).query(MAP).optional();
    }

    @Override
    public boolean existeCodigo(String codigo, Long ignorarId) {
        return db.sql("select count(*) from tipo_palete where codigo = ? and id <> coalesce(?, -1)")
                .params(codigo, ignorarId).query(Long.class).single() > 0;
    }

    @Override
    public boolean emUso(String codigo) {
        return db.sql("select (exists (select 1 from vale_palete where jsonb_exists(entregues, ?)) "
                        + "or exists (select 1 from movimento_palete where jsonb_exists(quantidades, ?)))")
                .params(codigo, codigo).query(Boolean.class).single();
    }

    @Override
    public TipoPalete salvar(TipoPalete t) {
        Long id = t.id() == null
                ? db.sql("insert into tipo_palete (codigo, nome, valor_reposicao, ordem, ativo, cd_id) values (?, ?, ?, ?, ?, ?) returning id")
                .params(t.codigo(), t.nome(), t.valorReposicao(), t.ordem(), t.ativo(), t.cdId()).query(Long.class).single()
                : db.sql("update tipo_palete set nome = ?, valor_reposicao = ?, ordem = ?, ativo = ?, cd_id = ? where id = ? returning id")
                .params(t.nome(), t.valorReposicao(), t.ordem(), t.ativo(), t.cdId(), t.id()).query(Long.class).single();
        return buscarPorId(id).orElseThrow();
    }

    @Override
    public void excluir(Long id) {
        db.sql("delete from tipo_palete where id = ?").param(id).update();
    }
}
