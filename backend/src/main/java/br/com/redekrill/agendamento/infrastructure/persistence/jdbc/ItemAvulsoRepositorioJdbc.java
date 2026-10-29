package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.ItemAvulsoRepositorio;
import br.com.redekrill.agendamento.domain.model.ItemAvulso;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ItemAvulsoRepositorioJdbc implements ItemAvulsoRepositorio {
    private static final RowMapper<ItemAvulso> MAP = (r, i) -> new ItemAvulso(r.getLong("id"), r.getLong("agendamento_id"),
            r.getObject("produto_id", Long.class), r.getString("codigo"), r.getString("descricao"), r.getBigDecimal("quantidade"),
            r.getString("unidade"), r.getString("observacao"), r.getString("criado_por"), r.getObject("criado_em", OffsetDateTime.class));
    private final JdbcClient db;

    public ItemAvulsoRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public List<ItemAvulso> listar(Long agendamentoId) {
        return db.sql("select * from recebimento_item_avulso where agendamento_id = ? order by id").param(agendamentoId).query(MAP).list();
    }

    @Override
    public ItemAvulso salvar(ItemAvulso i) {
        Long id = db.sql("""
                insert into recebimento_item_avulso (agendamento_id, produto_id, codigo, descricao, quantidade, unidade, observacao, criado_por, criado_em)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?) returning id""")
                .params(i.agendamentoId(), i.produtoId(), i.codigo(), i.descricao(), i.quantidade(), i.unidade(), i.observacao(),
                        i.criadoPor(), i.criadoEm()).query(Long.class).single();
        return db.sql("select * from recebimento_item_avulso where id = ?").param(id).query(MAP).single();
    }
}
