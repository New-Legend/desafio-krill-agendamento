package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.TaxaDescargaRepositorio;
import br.com.redekrill.agendamento.domain.model.TaxaDescarga;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TaxaDescargaRepositorioJdbc implements TaxaDescargaRepositorio {
    private static final RowMapper<TaxaDescarga> MAP = (r, i) -> new TaxaDescarga(r.getLong("id"), r.getString("modal"),
            r.getString("nome"), r.getBigDecimal("valor_por_palete"), r.getBoolean("ativo"));
    private final JdbcClient db;

    public TaxaDescargaRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public List<TaxaDescarga> listar() {
        return db.sql("select * from taxa_descarga order by ativo desc, valor_por_palete, modal").query(MAP).list();
    }

    @Override
    public Optional<TaxaDescarga> buscarPorId(Long id) {
        return db.sql("select * from taxa_descarga where id = ?").param(id).query(MAP).optional();
    }

    @Override
    public boolean existeModal(String modal, Long ignorarId) {
        return db.sql("select count(*) from taxa_descarga where modal = ? and id <> coalesce(?, -1)")
                .params(modal, ignorarId).query(Long.class).single() > 0;
    }

    @Override
    public TaxaDescarga salvar(TaxaDescarga t) {
        Long id = t.id() == null
                ? db.sql("insert into taxa_descarga (modal, nome, valor_por_palete, ativo) values (?, ?, ?, ?) returning id")
                .params(t.modal(), t.nome(), t.valorPorPalete(), t.ativo()).query(Long.class).single()
                : db.sql("update taxa_descarga set nome = ?, valor_por_palete = ?, ativo = ? where id = ? returning id")
                .params(t.nome(), t.valorPorPalete(), t.ativo(), t.id()).query(Long.class).single();
        return buscarPorId(id).orElseThrow();
    }
}
