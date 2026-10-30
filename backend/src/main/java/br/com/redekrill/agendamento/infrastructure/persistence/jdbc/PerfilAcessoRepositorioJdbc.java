package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.PerfilAcessoRepositorio;
import br.com.redekrill.agendamento.domain.model.PerfilAcesso;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Component
public class PerfilAcessoRepositorioJdbc implements PerfilAcessoRepositorio {
    private static final String SELECT = "select p.*, (select count(*) from usuario u where u.perfil_id = p.id and u.ativo) as usuarios, "
            + "coalesce((select string_agg(codigo::text, ',') from perfil_permissao pp where pp.perfil_id = p.id), '') as codigos "
            + "from perfil_acesso p ";
    private final JdbcClient db;

    public PerfilAcessoRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    private static PerfilAcesso mapear(java.sql.ResultSet r, int i) throws java.sql.SQLException {
        var codigos = new HashSet<Integer>();
        for (String c : r.getString("codigos").split(",")) if (!c.isBlank()) codigos.add(Integer.parseInt(c));
        return new PerfilAcesso(r.getLong("id"), r.getString("nome"), r.getString("descricao"), r.getBoolean("ativo"),
                codigos, r.getInt("usuarios"));
    }

    @Override
    public List<PerfilAcesso> listar() {
        return db.sql(SELECT + "order by p.ativo desc, p.id").query(PerfilAcessoRepositorioJdbc::mapear).list();
    }

    @Override
    public Optional<PerfilAcesso> buscarPorId(Long id) {
        return db.sql(SELECT + "where p.id = ?").param(id).query(PerfilAcessoRepositorioJdbc::mapear).optional();
    }

    @Override
    public Optional<PerfilAcesso> buscarPorNome(String nome) {
        return db.sql(SELECT + "where lower(p.nome) = lower(?)").param(nome).query(PerfilAcessoRepositorioJdbc::mapear).optional();
    }

    @Override
    public PerfilAcesso salvar(PerfilAcesso p) {
        Long id = p.id() == null
                ? db.sql("insert into perfil_acesso (nome, descricao, ativo) values (?, ?, ?) returning id")
                .params(p.nome(), p.descricao(), p.ativo()).query(Long.class).single()
                : db.sql("update perfil_acesso set nome = ?, descricao = ?, ativo = ? where id = ? returning id")
                .params(p.nome(), p.descricao(), p.ativo(), p.id()).query(Long.class).single();
        db.sql("delete from perfil_permissao where perfil_id = ?").param(id).update();
        for (Integer c : p.permissoes()) db.sql("insert into perfil_permissao (perfil_id, codigo) values (?, ?)").params(id, c).update();
        return buscarPorId(id).orElseThrow();
    }
}
