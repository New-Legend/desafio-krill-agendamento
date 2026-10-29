package br.com.redekrill.agendamento.infrastructure.mail;

import br.com.redekrill.agendamento.application.port.CanalEmailDocumentos;
import br.com.redekrill.agendamento.infrastructure.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** E-mail de documentos (cobrança/recibo de paletes) pelo mesmo SMTP do sistema (Mailpit no Docker). Síncrono para registrar o resultado. */
@Component
public class CanalEmailDocumentosSmtp implements CanalEmailDocumentos {
    private static final Logger log = LoggerFactory.getLogger(CanalEmailDocumentosSmtp.class);
    private final JavaMailSender sender;
    private final String remetente;

    public CanalEmailDocumentosSmtp(JavaMailSender sender, AppProperties props) {
        this.sender = sender;
        this.remetente = props.mail().remetente();
    }

    @Override
    public String provedor() {
        return "smtp";
    }

    @Override
    public Resultado enviar(String para, String assunto, String html, String texto) {
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
            h.setFrom(remetente);
            h.setTo(para);
            h.setSubject(assunto);
            h.setText(texto, html);
            h.addInline("logo", new ClassPathResource("email/rede-krill-logo.png"), "image/png");
            sender.send(msg);
            log.info("E-mail '{}' enviado para {}", assunto, para);
            return new Resultado(true, null);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail para {}: {}", para, e.getMessage());
            return new Resultado(false, "Falha no envio: " + e.getClass().getSimpleName());
        }
    }
}
