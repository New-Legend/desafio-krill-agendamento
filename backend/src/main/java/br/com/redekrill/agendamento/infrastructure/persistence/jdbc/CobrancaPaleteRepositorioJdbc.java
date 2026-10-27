package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.CobrancaPaleteRepositorio;
import br.com.redekrill.agendamento.domain.model.CobrancaPalete;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class CobrancaPaleteRepositorioJdbc implements CobrancaPaleteRepositorio {
    private static final RowMapper<CobrancaPalete> MAP = (r, i) -> new CobrancaPalete(r.getLong("id"), r.getString("fornecedor"),
            r.getInt("quantidade"), r.getBigDecimal("valor_unitario"), r.getObject("vencimento", LocalDate.class),
            r.getString("observacao"), r.getString("criado_por"), r.getObject("criado_em", OffsetDateTime.class),
            CobrancaPalete.Status.valueOf(r.getString("status")), r.getString("recibo_numero"),
            r.getString("forma_pagamento") == null ? null : CobrancaPalete.FormaPagamento.valueOf(r.getString("forma_pagamento")),
            r.getObject("pago_em", OffsetDateTime.class), r.getString("recebido_por"), r.getString("motivo_cancelamento"));
    private final JdbcClient db;

    public CobrancaPaleteRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public CobrancaPalete salvar(CobrancaPalete c) {
        Long id = c.getId() == null
                ? db.sql("insert into cobranca_palete (fornecedor, quantidade, valor_unitario, vencimento, status, observacao, "
                        + "criado_por, criado_em) values (?, ?, ?, ?, ?, ?, ?, ?) returning id")
                .params(c.getFornecedor(), c.getQuantidade(), c.getValorUnitario(), c.getVencimento(), c.getStatus().name(),
                        c.getObservacao(), c.getCriadoPor(), c.getCriadoEm()).query(Long.class).single()
                : db.sql("update cobranca_palete set status = ?, recibo_numero = ?, forma_pagamento = ?, pago_em = ?, "
                        + "recebido_por = ?, motivo_cancelamento = ? where id = ? returning id")
                .params(c.getStatus().name(), c.getReciboNumero(),
                        c.getFormaPagamento() == null ? null : c.getFormaPagamento().name(), c.getPagoEm(), c.getRecebidoPor(),
                        c.getMotivoCancelamento(), c.getId()).query(Long.class).single();
        return buscarPorId(id).orElseThrow();
    }

    @Override
    public Optional<CobrancaPalete> buscarPorId(Long id) {
        return db.sql("select * from cobranca_palete where id = ?").param(id).query(MAP).optional();
    }

    @Override
    public List<CobrancaPalete> listar(String fornecedor, CobrancaPalete.Status status) {
        String f = fornecedor == null || fornecedor.isBlank() ? null : "%" + fornecedor.trim().toLowerCase() + "%";
        return db.sql("select * from cobranca_palete where (cast(? as varchar) is null or lower(fornecedor) like ?) "
                        + "and (cast(? as varchar) is null or status = ?) order by id desc")
                .params(f, f, status == null ? null : status.name(), status == null ? null : status.name()).query(MAP).list();
    }

    @Override
    public long proximoRecibo() {
        return db.sql("select nextval('recibo_palete_seq')").query(Long.class).single();
    }
}
