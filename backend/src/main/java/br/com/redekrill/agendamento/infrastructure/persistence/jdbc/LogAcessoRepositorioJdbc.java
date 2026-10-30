package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.LogAcessoRepositorio;
import br.com.redekrill.agendamento.domain.model.EventoAcesso;
import br.com.redekrill.agendamento.domain.model.LogAcesso;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Component
public class LogAcessoRepositorioJdbc implements LogAcessoRepositorio {
    private final JdbcClient db;

    public LogAcessoRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    private static String corte(String v, int max) {
        return v == null ? null : v.length() > max ? v.substring(0, max) : v;
    }

    @Override
    public void registrar(String usuario, Long usuarioId, EventoAcesso evento, String ip, String navegador) {
        db.sql("insert into acesso_log (usuario, usuario_id, evento, ip, navegador) values (?, ?, ?, ?, ?)")
                .params(corte(usuario, 60), usuarioId, evento.name(), corte(ip, 45), corte(navegador, 200)).update();
    }

    @Override
    public List<LogAcesso> buscar(String texto, EventoAcesso evento, Instant desde, int limite) {
        String like = texto == null ? null : "%" + texto.toLowerCase() + "%";
        String ev = evento == null ? null : evento.name();
        return db.sql("select * from acesso_log where criado_em >= ? and (cast(? as varchar) is null or evento = ?) "
                        + "and (cast(? as varchar) is null or lower(usuario) like ? or ip like ?) order by criado_em desc, id desc limit ?")
                .params(Timestamp.from(desde), ev, ev, like, like, like, limite)
                .query((r, i) -> new LogAcesso(r.getLong("id"), r.getString("usuario"), EventoAcesso.valueOf(r.getString("evento")),
                        r.getString("ip"), r.getString("navegador"), r.getTimestamp("criado_em").toInstant()))
                .list();
    }
}
