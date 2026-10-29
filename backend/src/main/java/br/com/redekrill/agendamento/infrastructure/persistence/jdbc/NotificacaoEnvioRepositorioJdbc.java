package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.NotificacaoEnvioRepositorio;
import br.com.redekrill.agendamento.domain.model.NotificacaoEnvio;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class NotificacaoEnvioRepositorioJdbc implements NotificacaoEnvioRepositorio {
    private static final RowMapper<NotificacaoEnvio> MAP = (r, i) -> new NotificacaoEnvio(r.getLong("id"),
            NotificacaoEnvio.Canal.valueOf(r.getString("canal")), r.getString("documento_tipo"), r.getLong("documento_id"),
            r.getString("destinatario"), r.getString("assunto"), r.getString("provedor"),
            NotificacaoEnvio.Status.valueOf(r.getString("status")), r.getString("erro"), r.getString("usuario"),
            r.getObject("criado_em", OffsetDateTime.class));
    private final JdbcClient db;

    public NotificacaoEnvioRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public NotificacaoEnvio salvar(NotificacaoEnvio n) {
        Long id = db.sql("insert into notificacao_envio (canal, documento_tipo, documento_id, destinatario, assunto, provedor, "
                        + "status, erro, usuario, criado_em) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id")
                .params(n.canal().name(), n.documentoTipo(), n.documentoId(), n.destinatario(), n.assunto(), n.provedor(),
                        n.status().name(), n.erro(), n.usuario(), n.criadoEm())
                .query(Long.class).single();
        return db.sql("select * from notificacao_envio where id = ?").param(id).query(MAP).single();
    }

    @Override
    public List<NotificacaoEnvio> listar(String documentoTipo, Long documentoId) {
        return db.sql("select * from notificacao_envio where documento_tipo = ? and documento_id = ? order by criado_em desc, id desc limit 50")
                .params(documentoTipo, documentoId).query(MAP).list();
    }
}
