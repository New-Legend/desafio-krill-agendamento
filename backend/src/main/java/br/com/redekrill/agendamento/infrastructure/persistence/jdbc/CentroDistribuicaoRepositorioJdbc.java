package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.domain.model.CentroDistribuicao;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CentroDistribuicaoRepositorioJdbc implements CentroDistribuicaoRepositorio {
    private static final RowMapper<CentroDistribuicao> MAP = (r, i) -> new CentroDistribuicao(r.getLong("id"),
            r.getString("codigo"), r.getString("nome"), r.getString("cidade"), r.getObject("capacidade_diaria_paletes", Integer.class),
            r.getObject("capacidade_faixa_paletes", Integer.class), r.getBoolean("ativo"));
    private final JdbcClient db;

    public CentroDistribuicaoRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public List<CentroDistribuicao> listar() {
        return db.sql("select * from centro_distribuicao order by id").query(MAP).list();
    }

    @Override
    public Optional<CentroDistribuicao> buscarPorId(Long id) {
        return db.sql("select * from centro_distribuicao where id = ?").param(id).query(MAP).optional();
    }

    @Override
    public boolean existeCodigo(String codigo, Long ignorarId) {
        return db.sql("select count(*) from centro_distribuicao where lower(codigo) = lower(?) and id <> coalesce(?, -1)")
                .params(codigo, ignorarId).query(Long.class).single() > 0;
    }

    @Override
    public CentroDistribuicao salvar(CentroDistribuicao c) {
        Long id = c.id() == null
                ? db.sql("insert into centro_distribuicao (codigo, nome, cidade, capacidade_diaria_paletes, capacidade_faixa_paletes, ativo) "
                        + "values (?, ?, ?, ?, ?, ?) returning id")
                .params(c.codigo(), c.nome(), c.cidade(), c.capacidadeDiariaPaletes(), c.capacidadeFaixaPaletes(), c.ativo())
                .query(Long.class).single()
                : db.sql("update centro_distribuicao set codigo = ?, nome = ?, cidade = ?, capacidade_diaria_paletes = ?, "
                        + "capacidade_faixa_paletes = ?, ativo = ? where id = ? returning id")
                .params(c.codigo(), c.nome(), c.cidade(), c.capacidadeDiariaPaletes(), c.capacidadeFaixaPaletes(), c.ativo(), c.id())
                .query(Long.class).single();
        return buscarPorId(id).orElseThrow();
    }
}
