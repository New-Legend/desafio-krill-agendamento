package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.UsuarioRepositorio;
import br.com.redekrill.agendamento.domain.model.UsuarioSistema;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class UsuarioRepositorioJdbc implements UsuarioRepositorio {
    private static final String SELECT = "select u.*, p.nome as perfil_nome from usuario u join perfil_acesso p on p.id = u.perfil_id ";
    private static final RowMapper<UsuarioSistema> MAP = (r, i) -> new UsuarioSistema(r.getLong("id"), r.getString("username"),
            r.getString("nome"), r.getLong("perfil_id"), r.getString("perfil_nome"), r.getBoolean("ativo"),
            r.getBoolean("trocar_senha"), instante(r.getTimestamp("ultimo_acesso_em")), instante(r.getTimestamp("criado_em")),
            r.getString("senha_hash"));
    private final JdbcClient db;

    public UsuarioRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    private static Instant instante(Timestamp t) {
        return t == null ? null : t.toInstant();
    }

    @Override
    public List<UsuarioSistema> listar() {
        return db.sql(SELECT + "order by u.ativo desc, lower(u.nome)").query(MAP).list();
    }

    @Override
    public Optional<UsuarioSistema> buscarPorId(Long id) {
        return db.sql(SELECT + "where u.id = ?").param(id).query(MAP).optional();
    }

    @Override
    public Optional<UsuarioSistema> buscarPorLogin(String login) {
        return db.sql(SELECT + "where lower(u.username) = lower(?)").param(login).query(MAP).optional();
    }

    @Override
    public UsuarioSistema salvar(UsuarioSistema u) {
        Long id = u.id() == null
                ? db.sql("insert into usuario (username, nome, senha_hash, perfil_id, ativo, trocar_senha) values (?, ?, ?, ?, ?, ?) returning id")
                .params(u.login(), u.nome(), u.senhaHash(), u.perfilId(), u.ativo(), u.trocarSenha()).query(Long.class).single()
                : db.sql("update usuario set nome = ?, senha_hash = ?, perfil_id = ?, ativo = ?, trocar_senha = ? where id = ? returning id")
                .params(u.nome(), u.senhaHash(), u.perfilId(), u.ativo(), u.trocarSenha(), u.id()).query(Long.class).single();
        return buscarPorId(id).orElseThrow();
    }

    @Override
    public void registrarAcesso(Long id, Instant quando) {
        db.sql("update usuario set ultimo_acesso_em = ? where id = ?").params(Timestamp.from(quando), id).update();
    }
}
