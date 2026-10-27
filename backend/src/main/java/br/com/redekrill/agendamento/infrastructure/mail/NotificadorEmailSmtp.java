package br.com.redekrill.agendamento.infrastructure.mail;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.port.NotificadorEmail;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.infrastructure.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/** Envio de e-mail ao fornecedor via SMTP (assíncrono: falha de e-mail não desfaz o agendamento). */
@Component
public class NotificadorEmailSmtp implements NotificadorEmail {

    private static final Logger log = LoggerFactory.getLogger(NotificadorEmailSmtp.class);
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender sender;
    private final String remetente;

    public NotificadorEmailSmtp(JavaMailSender sender, AppProperties props) {
        this.sender = sender;
        this.remetente = props.mail().remetente();
    }

    @Async
    @Override
    public void enviarConfirmacao(AgendamentoDetalhe d, String linkFornecedor) {
        Agendamento a = d.agendamento();
        enviar(a.getEmailFornecedor(), "Agendamento de entrega confirmado — Pedido " + a.getIdPedido(),
                "Agendamento de entrega confirmado", d, "#148a42", linkFornecedor);
    }

    @Async
    @Override
    public void enviarCancelamento(AgendamentoDetalhe d) {
        Agendamento a = d.agendamento();
        enviar(a.getEmailFornecedor(), "Agendamento de entrega cancelado — Pedido " + a.getIdPedido(),
                "Agendamento de entrega cancelado", d, "#dc2626", null);
    }

    private void enviar(String para, String assunto, String titulo, AgendamentoDetalhe d, String cor, String link) {
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
            h.setFrom(remetente);
            h.setTo(para);
            h.setSubject(assunto);
            h.setText(html(titulo, d, cor, link), true);
            h.addInline("logo", new org.springframework.core.io.ClassPathResource("email/rede-krill-logo.png"), "image/png");
            sender.send(msg);
            log.info("E-mail '{}' enviado para {}", assunto, para);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail para {}: {}", para, e.getMessage());
        }
    }

    static String html(String titulo, AgendamentoDetalhe d, String cor, String link) {
        Agendamento a = d.agendamento();
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#334155\">")
                .append("<div style=\"background:#020617;height:6px;border-bottom:2px solid #ffd21f\"></div>")
                .append("<img src=\"cid:logo\" alt=\"Rede Krill\" width=\"90\" height=\"83\" style=\"display:block;border:0\">")
                .append("<h2 style=\"color:").append(cor).append(";font-size:18px\">").append(titulo).append("</h2>")
                .append("<p>Rede Krill — Recebimento</p>")
                .append("<table cellpadding=\"6\" style=\"border-collapse:collapse;border:1px solid #e2e8f0\">");
        linha(sb, "Agendamento", "#" + a.getId());
        linha(sb, "Data", a.getData().format(BR));
        linha(sb, "Faixa horária", d.faixaDescricao());
        linha(sb, "ID do pedido", String.valueOf(a.getIdPedido()));
        linha(sb, "Fornecedor", a.getFornecedor());
        linha(sb, "Tipo de caminhão", d.tipoCaminhao());
        linha(sb, "Tipo de paletização", d.tipoPaletizacao());
        linha(sb, "Quantidade de paletes", String.valueOf(a.getQuantidadePaletes()));
        if (a.getObservacao() != null) linha(sb, "Observação", a.getObservacao());
        if (a.getMotivoCancelamento() != null) linha(sb, "Motivo do cancelamento", a.getMotivoCancelamento());
        sb.append("</table>");
        if (link != null) {
            sb.append("<p><a href=\"").append(HtmlUtils.htmlEscape(link)).append("\" style=\"display:inline-block;background:#148a42;color:#fff;")
                    .append("padding:8px 14px;border-radius:8px;text-decoration:none\">Confirmar presença e informar placa</a></p>");
        }
        sb.append("<p style=\"color:#64748b;font-size:12px\">Apresente o ID do pedido na portaria.</p></div>");
        return sb.toString();
    }

    private static void linha(StringBuilder sb, String rotulo, String valor) {
        sb.append("<tr><td style=\"background:#f1f5f9;border:1px solid #e2e8f0\"><b>").append(rotulo)
                .append("</b></td><td style=\"border:1px solid #e2e8f0\">").append(HtmlUtils.htmlEscape(valor))
                .append("</td></tr>");
    }
}
