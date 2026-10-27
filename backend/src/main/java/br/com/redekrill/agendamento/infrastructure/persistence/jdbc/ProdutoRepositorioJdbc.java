package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.ProdutoRepositorio;
import br.com.redekrill.agendamento.domain.model.Produto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProdutoRepositorioJdbc implements ProdutoRepositorio {
    private static final RowMapper<Produto> MAP = (r, i) -> new Produto(r.getLong("id"), r.getString("codigo"),
            r.getString("ean"), r.getString("descricao"), r.getString("unidade"), r.getBigDecimal("peso_bruto_kg"),
            r.getObject("itens_por_palete", Integer.class), r.getBoolean("ativo"));
    private final JdbcClient db;

    public ProdutoRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public List<Produto> listar(String busca, Boolean semPeso) {
        String b = busca == null || busca.isBlank() ? null : "%" + busca.trim().toLowerCase() + "%";
        return db.sql("select * from produto where (cast(? as varchar) is null or lower(descricao) like ? or lower(codigo) like ? "
                        + "or ean like ?) and (cast(? as boolean) is not true or peso_bruto_kg is null) order by descricao limit 500")
                .params(b, b, b, b, semPeso).query(MAP).list();
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {
        return db.sql("select * from produto where id = ?").param(id).query(MAP).optional();
    }

    @Override
    public Optional<Produto> buscarPorCodigo(String codigo) {
        return db.sql("select * from produto where codigo = ?").param(codigo).query(MAP).optional();
    }

    @Override
    public Optional<Produto> buscarPorEan(String ean) {
        return db.sql("select * from produto where ean = ? order by id limit 1").param(ean).query(MAP).optional();
    }

    @Override
    public Produto salvar(Produto p) {
        Long id = p.id() == null
                ? db.sql("insert into produto (codigo, ean, descricao, unidade, peso_bruto_kg, itens_por_palete, ativo) "
                        + "values (?, ?, ?, ?, ?, ?, ?) returning id")
                .params(p.codigo(), p.ean(), p.descricao(), p.unidade(), p.pesoBrutoKg(), p.itensPorPalete(), p.ativo())
                .query(Long.class).single()
                : db.sql("update produto set codigo = ?, ean = ?, descricao = ?, unidade = ?, peso_bruto_kg = ?, "
                        + "itens_por_palete = ?, ativo = ? where id = ? returning id")
                .params(p.codigo(), p.ean(), p.descricao(), p.unidade(), p.pesoBrutoKg(), p.itensPorPalete(), p.ativo(), p.id())
                .query(Long.class).single();
        return buscarPorId(id).orElseThrow();
    }
}
