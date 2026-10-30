package br.com.redekrill.agendamento.infrastructure.push;

import br.com.redekrill.agendamento.application.port.NotificadorPush;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.PushInscricaoEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.PushInscricaoJpa;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Envia notificações Web Push (VAPID) aos navegadores inscritos de usuários ativos cujo perfil tem a permissão
 * 1102 (Agendamentos: consultar), como o Gestão CD faz com agendamento_ver.
 */
@Component
public class NotificadorWebPush implements NotificadorPush {

    private static final Logger log = LoggerFactory.getLogger(NotificadorWebPush.class);

    private final PushInscricaoJpa inscricoes;
    private final ChavesVapid chaves;
    private final ObjectMapper mapper;
    private final org.springframework.jdbc.core.simple.JdbcClient db;
    private volatile PushService service;

    public NotificadorWebPush(PushInscricaoJpa inscricoes, ChavesVapid chaves, ObjectMapper mapper,
                              org.springframework.jdbc.core.simple.JdbcClient db) {
        this.db = db;
        this.inscricoes = inscricoes;
        this.chaves = chaves;
        this.mapper = mapper;
    }

    private PushService service() throws Exception {
        if (service == null) {
            service = new PushService(chaves.publica(), chaves.privada(), chaves.subject());
        }
        return service;
    }

    public void inscrever(String usuario, String endpoint, String p256dh, String auth) {
        PushInscricaoEntity e = inscricoes.findByEndpoint(endpoint).orElseGet(PushInscricaoEntity::new);
        e.usuario = usuario;
        e.endpoint = endpoint;
        e.p256dh = p256dh;
        e.auth = auth;
        if (e.criadoEm == null) e.criadoEm = OffsetDateTime.now();
        inscricoes.save(e);
    }

    public void remover(String endpoint) {
        inscricoes.deleteByEndpoint(endpoint);
    }

    @Async
    @Override
    public void notificarTodos(String titulo, String mensagem, String url) {
        String payload;
        try {
            payload = mapper.writeValueAsString(Map.of("title", titulo, "body", mensagem, "url", url));
        } catch (Exception e) {
            return;
        }
        var destinatarios = new java.util.HashSet<>(db.sql("select lower(u.username) from usuario u join perfil_acesso p on p.id = u.perfil_id "
                + "join perfil_permissao pp on pp.perfil_id = p.id where u.ativo and p.ativo and pp.codigo = ?")
                .param(br.com.redekrill.agendamento.domain.model.Permissao.AGENDAMENTOS_VER.codigo()).query(String.class).list());
        for (PushInscricaoEntity i : inscricoes.findAll()) {
            if (i.usuario == null || !destinatarios.contains(i.usuario.toLowerCase())) continue;
            try {
                HttpResponse r = service().send(new Notification(i.endpoint, i.p256dh, i.auth, payload));
                int status = r.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    inscricoes.delete(i);
                    log.info("Inscrição push expirada removida ({})", i.usuario);
                } else if (status >= 300) {
                    log.warn("Push recusado com status {} para {}", status, i.usuario);
                }
            } catch (Exception e) {
                log.warn("Falha ao enviar push para {}: {}", i.usuario, e.getMessage());
            }
        }
    }
}
