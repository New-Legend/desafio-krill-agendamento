package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.FornecedorRepositorio;
import br.com.redekrill.agendamento.domain.model.Fornecedor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FornecedorRepositorioJdbc implements FornecedorRepositorio {
    private static final RowMapper<Fornecedor> MAP = (r, i) -> new Fornecedor(r.getLong("id"), r.getString("cnpj"),
            r.getString("razao_social"), r.getString("nome_fantasia"), r.getString("email"), r.getString("telefone"),
            r.getString("contato"), r.getBoolean("ativo"));
    private final JdbcClient db;

    public FornecedorRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public List<Fornecedor> listar(String busca, Boolean ativo) {
        String b = busca == null || busca.isBlank() ? null : "%" + busca.trim().toLowerCase() + "%";
        String d = busca == null ? null : busca.replaceAll("\\D", "");
        return db.sql("select * from fornecedor where (cast(? as varchar) is null or lower(razao_social) like ? "
                        + "or lower(coalesce(nome_fantasia, '')) like ? or (? <> '' and cnpj like ?)) "
                        + "and (cast(? as boolean) is null or ativo = ?) order by razao_social")
                .params(b, b, b, d == null ? "" : d, "%" + (d == null ? "" : d) + "%", ativo, ativo).query(MAP).list();
    }

    @Override
    public Optional<Fornecedor> buscarPorId(Long id) {
        return db.sql("select * from fornecedor where id = ?").param(id).query(MAP).optional();
    }

    @Override
    public Optional<Fornecedor> buscarPorCnpj(String cnpj) {
        return db.sql("select * from fornecedor where cnpj = ?").param(cnpj).query(MAP).optional();
    }

    @Override
    public Fornecedor salvar(Fornecedor f) {
        Long id = f.id() == null
                ? db.sql("insert into fornecedor (cnpj, razao_social, nome_fantasia, email, telefone, contato, ativo) "
                        + "values (?, ?, ?, ?, ?, ?, ?) returning id")
                .params(f.cnpj(), f.razaoSocial(), f.nomeFantasia(), f.email(), f.telefone(), f.contato(), f.ativo())
                .query(Long.class).single()
                : db.sql("update fornecedor set cnpj = ?, razao_social = ?, nome_fantasia = ?, email = ?, telefone = ?, "
                        + "contato = ?, ativo = ? where id = ? returning id")
                .params(f.cnpj(), f.razaoSocial(), f.nomeFantasia(), f.email(), f.telefone(), f.contato(), f.ativo(), f.id())
                .query(Long.class).single();
        return buscarPorId(id).orElseThrow();
    }
}
