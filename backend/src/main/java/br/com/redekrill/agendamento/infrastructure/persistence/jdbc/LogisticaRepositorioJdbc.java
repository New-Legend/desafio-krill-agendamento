package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.LogisticaRepositorio;
import br.com.redekrill.agendamento.domain.model.LogisticaAgendamento;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LogisticaRepositorioJdbc implements LogisticaRepositorio {
    private static final RowMapper<LogisticaAgendamento> MAP = (r, i) -> new LogisticaAgendamento(r.getLong("agendamento_id"),
            r.getString("loja"), r.getString("transportadora"), r.getObject("doca_id", Long.class),
            r.getObject("doca_em", OffsetDateTime.class), r.getString("modal_descarga"));
    /** Status que ocupam capacidade (mesma regra de StatusAgendamento.ocupaCapacidade). */
    private static final String ATIVOS = "a.status not in ('CANCELADO', 'NAO_COMPARECEU')";
    private final JdbcClient db;

    public LogisticaRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public Optional<LogisticaAgendamento> buscar(Long agendamentoId) {
        return db.sql("select * from agendamento_logistica where agendamento_id = ?").param(agendamentoId).query(MAP).optional();
    }

    @Override
    public Map<Long, LogisticaAgendamento> porAgendamentos(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return db.sql("select * from agendamento_logistica where agendamento_id = any(?)")
                .param(ids.toArray(Long[]::new)).query(MAP).list().stream()
                .collect(Collectors.toMap(LogisticaAgendamento::agendamentoId, Function.identity()));
    }

    @Override
    public LogisticaAgendamento salvar(LogisticaAgendamento l) {
        db.sql("""
                insert into agendamento_logistica (agendamento_id, loja, transportadora, doca_id, doca_em, modal_descarga)
                values (?, ?, ?, ?, ?, ?)
                on conflict (agendamento_id) do update set loja = excluded.loja, transportadora = excluded.transportadora,
                    doca_id = excluded.doca_id, doca_em = excluded.doca_em, modal_descarga = excluded.modal_descarga""")
                .params(l.agendamentoId(), l.loja(), l.transportadora(), l.docaId(), l.docaEm(), l.modalDescarga()).update();
        return buscar(l.agendamentoId()).orElseThrow();
    }

    @Override
    public Optional<Long> ocupanteDaDoca(Long docaId, Long ignorarAgendamentoId) {
        return db.sql("""
                select a.id from agendamento_logistica l join agendamento a on a.id = l.agendamento_id
                where l.doca_id = ? and a.status in ('CHEGOU', 'EM_RECEBIMENTO') and a.id <> coalesce(?, -1)
                order by l.doca_em desc limit 1""").params(docaId, ignorarAgendamentoId).query(Long.class).optional();
    }

    @Override
    public Consumo consumo(LocalDate data, Long cdId, String loja) {
        String filtro = loja == null
                ? "coalesce(a.cd_id, 1) = ? and l.loja is null"
                : "lower(l.loja) = lower(?)";
        return db.sql("select coalesce(sum(a.paletes_equivalentes), 0) as paletes, count(*) as caminhoes "
                        + "from agendamento a left join agendamento_logistica l on l.agendamento_id = a.id "
                        + "where a.data = ? and " + ATIVOS + " and " + filtro)
                .params(data, loja == null ? cdId : loja)
                .query((r, i) -> new Consumo(r.getInt("paletes"), r.getInt("caminhoes"))).single();
    }

    @Override
    public List<SaldoTransportadora> saldosPorTransportadora() {
        return db.sql("""
                with v as (
                  select coalesce(nullif(l.transportadora, ''), vp.fornecedor) as nome,
                         (select coalesce(sum(value::int), 0) from jsonb_each_text(vp.entregues)) as entregues,
                         (select coalesce(sum(value::int), 0) from jsonb_each_text(vp.devolvidos)) as devolvidos
                  from vale_palete vp left join agendamento_logistica l on l.agendamento_id = vp.agendamento_id
                  where vp.status <> 'CANCELADO')
                select nome, count(*) as vales, sum(entregues) as entregues, sum(devolvidos) as devolvidos,
                       sum(entregues) - sum(devolvidos) as saldo
                from v group by nome order by saldo desc, nome""")
                .query((r, i) -> new SaldoTransportadora(r.getString("nome"), r.getInt("vales"), r.getInt("entregues"),
                        r.getInt("devolvidos"), r.getInt("saldo"))).list();
    }
}
