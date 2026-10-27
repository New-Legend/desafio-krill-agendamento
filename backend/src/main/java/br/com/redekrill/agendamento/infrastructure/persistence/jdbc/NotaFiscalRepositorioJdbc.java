package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.NotaFiscalRepositorio;
import br.com.redekrill.agendamento.domain.model.NotaFiscal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NotaFiscalRepositorioJdbc implements NotaFiscalRepositorio {
    private final JdbcClient db;

    public NotaFiscalRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public NotaFiscal salvar(NotaFiscal n) {
        Long id = db.sql("insert into nota_fiscal (agendamento_id, chave, numero, serie, emitente_cnpj, emitente_nome, emissao, "
                        + "valor_total, peso_bruto_kg, peso_calculado_kg, volumes, importado_por, importado_em) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id")
                .params(n.agendamentoId(), n.chave(), n.numero(), n.serie(), n.emitenteCnpj(), n.emitenteNome(), n.emissao(),
                        n.valorTotal(), n.pesoBrutoKg(), n.pesoCalculadoKg(), n.volumes(), n.importadoPor(), n.importadoEm())
                .query(Long.class).single();
        for (NotaFiscal.Item i : n.itens()) {
            db.sql("insert into nota_fiscal_item (nota_id, numero_item, codigo, ean, descricao, ncm, unidade, quantidade, "
                            + "valor_unitario, valor_total, peso_kg, produto_id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                    .params(id, i.numeroItem(), i.codigo(), i.ean(), i.descricao(), i.ncm(), i.unidade(), i.quantidade(),
                            i.valorUnitario(), i.valorTotal(), i.pesoKg(), i.produtoId()).update();
        }
        return buscarPorId(id).orElseThrow();
    }

    @Override
    public Optional<NotaFiscal> buscarPorId(Long id) {
        return carregar("where id = ?", List.of(id)).stream().findFirst();
    }

    @Override
    public boolean existeChave(String chave) {
        return db.sql("select count(*) from nota_fiscal where chave = ?").param(chave).query(Long.class).single() > 0;
    }

    @Override
    public List<NotaFiscal> listarPorAgendamento(Long agendamentoId) {
        return carregar("where agendamento_id = ?", List.of(agendamentoId));
    }

    @Override
    public List<NotaFiscal> listarPorAgendamentos(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return carregar("where agendamento_id in (" + ids.stream().map(x -> "?").collect(Collectors.joining(",")) + ")",
                List.copyOf(ids));
    }

    @Override
    public void excluir(Long id) {
        db.sql("delete from nota_fiscal where id = ?").param(id).update();
    }

    private List<NotaFiscal> carregar(String where, List<?> params) {
        List<NotaFiscal> notas = db.sql("select * from nota_fiscal " + where + " order by id").params(params)
                .query((r, i) -> nota(r)).list();
        if (notas.isEmpty()) return notas;
        Map<Long, List<NotaFiscal.Item>> itens = db.sql("select * from nota_fiscal_item where nota_id in ("
                        + notas.stream().map(n -> String.valueOf(n.id())).collect(Collectors.joining(",")) + ") order by numero_item")
                .query((r, i) -> Map.entry(r.getLong("nota_id"), item(r))).list().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        return notas.stream().map(n -> new NotaFiscal(n.id(), n.agendamentoId(), n.chave(), n.numero(), n.serie(),
                n.emitenteCnpj(), n.emitenteNome(), n.emissao(), n.valorTotal(), n.pesoBrutoKg(), n.volumes(),
                itens.getOrDefault(n.id(), List.of()), n.importadoPor(), n.importadoEm())).toList();
    }

    private static NotaFiscal nota(ResultSet r) throws SQLException {
        return new NotaFiscal(r.getLong("id"), r.getLong("agendamento_id"), r.getString("chave"), r.getString("numero"),
                r.getString("serie"), r.getString("emitente_cnpj"), r.getString("emitente_nome"),
                r.getObject("emissao", OffsetDateTime.class), r.getBigDecimal("valor_total"), r.getBigDecimal("peso_bruto_kg"),
                r.getObject("volumes", Integer.class), List.of(), r.getString("importado_por"),
                r.getObject("importado_em", OffsetDateTime.class));
    }

    private static NotaFiscal.Item item(ResultSet r) throws SQLException {
        return new NotaFiscal.Item(r.getInt("numero_item"), r.getString("codigo"), r.getString("ean"), r.getString("descricao"),
                r.getString("ncm"), r.getString("unidade"), r.getBigDecimal("quantidade"), r.getBigDecimal("valor_unitario"),
                r.getBigDecimal("valor_total"), r.getBigDecimal("peso_kg"), r.getObject("produto_id", Long.class));
    }
}
