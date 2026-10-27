package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.MovimentoPaleteRepositorio;
import br.com.redekrill.agendamento.domain.model.MovimentoPalete;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class MovimentoPaleteRepositorioJdbc implements MovimentoPaleteRepositorio {
    private static final RowMapper<MovimentoPalete> MAP = (r, i) -> new MovimentoPalete(r.getLong("id"),
            r.getObject("data", LocalDate.class), MovimentoPalete.Tipo.valueOf(r.getString("tipo")), r.getLong("cd_id"),
            r.getString("loja"), r.getString("romaneio"),
            new ValePalete.Quantidades(r.getInt("pbr"), r.getInt("chep"), r.getInt("chapatex"), r.getInt("descartaveis")),
            r.getString("observacao"), r.getString("criado_por"), r.getObject("criado_em", OffsetDateTime.class));
    private final JdbcClient db;

    public MovimentoPaleteRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public MovimentoPalete salvar(MovimentoPalete m) {
        Long id = db.sql("insert into movimento_palete (data, tipo, cd_id, loja, romaneio, pbr, chep, chapatex, descartaveis, "
                        + "observacao, criado_por, criado_em) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id")
                .params(m.data(), m.tipo().name(), m.cdId(), m.loja(), m.romaneio(), m.quantidades().pbr(), m.quantidades().chep(),
                        m.quantidades().chapatex(), m.quantidades().descartaveis(), m.observacao(), m.criadoPor(), m.criadoEm())
                .query(Long.class).single();
        return db.sql("select * from movimento_palete where id = ?").param(id).query(MAP).single();
    }

    @Override
    public List<MovimentoPalete> listar(String loja, LocalDate inicio, LocalDate fim) {
        String l = loja == null || loja.isBlank() ? null : "%" + loja.trim().toLowerCase() + "%";
        return db.sql("select * from movimento_palete where (cast(? as varchar) is null or lower(loja) like ?) "
                        + "and (cast(? as date) is null or data >= ?) and (cast(? as date) is null or data <= ?) order by data desc, id desc")
                .params(l, l, inicio, inicio, fim, fim).query(MAP).list();
    }

    @Override
    public long proximoRomaneio() {
        return db.sql("select nextval('romaneio_palete_seq')").query(Long.class).single();
    }
}
