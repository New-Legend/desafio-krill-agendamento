package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.MovimentoPaleteRepositorio;
import br.com.redekrill.agendamento.domain.model.MovimentoPalete;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
public class MovimentoPaleteRepositorioJdbc implements MovimentoPaleteRepositorio {
    private static final TypeReference<Map<String, Integer>> MAPA = new TypeReference<>() { };
    private final JdbcClient db;
    private final ObjectMapper json;
    private final RowMapper<MovimentoPalete> map;

    public MovimentoPaleteRepositorioJdbc(JdbcClient db, ObjectMapper json) {
        this.db = db;
        this.json = json;
        this.map = (r, i) -> new MovimentoPalete(r.getLong("id"),
                r.getObject("data", LocalDate.class), MovimentoPalete.Tipo.valueOf(r.getString("tipo")), r.getLong("cd_id"),
                r.getString("loja"), r.getString("romaneio"), new ValePalete.Quantidades(ler(r.getString("quantidades"))),
                r.getString("observacao"), r.getString("criado_por"), r.getObject("criado_em", OffsetDateTime.class));
    }

    private Map<String, Integer> ler(String s) {
        try {
            return s == null ? Map.of() : json.readValue(s, MAPA);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Quantidades inválidas no movimento de paletes", e);
        }
    }

    private String escrever(ValePalete.Quantidades q) {
        try {
            return json.writeValueAsString(q.itens());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public MovimentoPalete salvar(MovimentoPalete m) {
        Long id = db.sql("insert into movimento_palete (data, tipo, cd_id, loja, romaneio, quantidades, "
                        + "observacao, criado_por, criado_em) values (?, ?, ?, ?, ?, cast(? as jsonb), ?, ?, ?) returning id")
                .params(m.data(), m.tipo().name(), m.cdId(), m.loja(), m.romaneio(), escrever(m.quantidades()),
                        m.observacao(), m.criadoPor(), m.criadoEm())
                .query(Long.class).single();
        return db.sql("select * from movimento_palete where id = ?").param(id).query(map).single();
    }

    @Override
    public List<MovimentoPalete> listar(String loja, LocalDate inicio, LocalDate fim) {
        String l = loja == null || loja.isBlank() ? null : "%" + loja.trim().toLowerCase() + "%";
        return db.sql("select * from movimento_palete where (cast(? as varchar) is null or lower(loja) like ?) "
                        + "and (cast(? as date) is null or data >= ?) and (cast(? as date) is null or data <= ?) order by data desc, id desc")
                .params(l, l, inicio, inicio, fim, fim).query(map).list();
    }

    @Override
    public long proximoRomaneio() {
        return db.sql("select nextval('romaneio_palete_seq')").query(Long.class).single();
    }
}
