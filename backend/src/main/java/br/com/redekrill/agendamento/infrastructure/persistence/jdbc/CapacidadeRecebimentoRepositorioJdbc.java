package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.CapacidadeRecebimentoRepositorio;
import br.com.redekrill.agendamento.domain.model.CapacidadeRecebimento;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CapacidadeRecebimentoRepositorioJdbc implements CapacidadeRecebimentoRepositorio {
    private static final RowMapper<CapacidadeRecebimento> MAP = (r, i) -> new CapacidadeRecebimento(r.getLong("id"),
            r.getObject("cd_id", Long.class), r.getString("loja"), r.getObject("dia_semana", Integer.class),
            r.getObject("paletes", Integer.class), r.getObject("caminhoes", Integer.class));
    private final JdbcClient db;

    public CapacidadeRecebimentoRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public List<CapacidadeRecebimento> listar() {
        return db.sql("select * from capacidade_recebimento order by loja nulls first, cd_id, dia_semana nulls first")
                .query(MAP).list();
    }

    @Override
    public Optional<CapacidadeRecebimento> buscarPorId(Long id) {
        return db.sql("select * from capacidade_recebimento where id = ?").param(id).query(MAP).optional();
    }

    @Override
    public CapacidadeRecebimento salvar(CapacidadeRecebimento c) {
        Long id = c.id() == null
                ? db.sql("insert into capacidade_recebimento (cd_id, loja, dia_semana, paletes, caminhoes) values (?, ?, ?, ?, ?) returning id")
                .params(c.cdId(), c.loja(), c.diaSemana(), c.paletes(), c.caminhoes()).query(Long.class).single()
                : db.sql("update capacidade_recebimento set cd_id = ?, loja = ?, dia_semana = ?, paletes = ?, caminhoes = ? where id = ? returning id")
                .params(c.cdId(), c.loja(), c.diaSemana(), c.paletes(), c.caminhoes(), c.id()).query(Long.class).single();
        return buscarPorId(id).orElseThrow();
    }

    @Override
    public void excluir(Long id) {
        db.sql("delete from capacidade_recebimento where id = ?").param(id).update();
    }
}
