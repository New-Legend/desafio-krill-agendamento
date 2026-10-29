package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.DocaRepositorio;
import br.com.redekrill.agendamento.domain.model.Doca;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DocaRepositorioJdbc implements DocaRepositorio {
    private static final RowMapper<Doca> MAP = (r, i) -> new Doca(r.getLong("id"), r.getLong("cd_id"), r.getString("codigo"),
            r.getString("nome"), r.getBoolean("ativa"));
    private final JdbcClient db;

    public DocaRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public List<Doca> listar(Long cdId) {
        return db.sql("select * from doca where (cast(? as bigint) is null or cd_id = ?) order by cd_id, codigo")
                .params(cdId, cdId).query(MAP).list();
    }

    @Override
    public Optional<Doca> buscarPorId(Long id) {
        return db.sql("select * from doca where id = ?").param(id).query(MAP).optional();
    }

    @Override
    public boolean existeCodigo(Long cdId, String codigo, Long ignorarId) {
        return db.sql("select count(*) from doca where cd_id = ? and upper(codigo) = upper(?) and id <> coalesce(?, -1)")
                .params(cdId, codigo, ignorarId).query(Long.class).single() > 0;
    }

    @Override
    public Doca salvar(Doca d) {
        Long id = d.id() == null
                ? db.sql("insert into doca (cd_id, codigo, nome, ativa) values (?, ?, ?, ?) returning id")
                .params(d.cdId(), d.codigo(), d.nome(), d.ativa()).query(Long.class).single()
                : db.sql("update doca set cd_id = ?, codigo = ?, nome = ?, ativa = ? where id = ? returning id")
                .params(d.cdId(), d.codigo(), d.nome(), d.ativa(), d.id()).query(Long.class).single();
        return buscarPorId(id).orElseThrow();
    }
}
