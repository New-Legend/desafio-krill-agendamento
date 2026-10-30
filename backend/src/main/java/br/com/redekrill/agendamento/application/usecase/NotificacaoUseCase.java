package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.CanalEmailDocumentos;
import br.com.redekrill.agendamento.application.port.FornecedorRepositorio;
import br.com.redekrill.agendamento.application.port.NotificacaoEnvioRepositorio;
import br.com.redekrill.agendamento.application.port.NotificadorWhatsApp;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.CobrancaPalete;
import br.com.redekrill.agendamento.domain.model.Fornecedor;
import br.com.redekrill.agendamento.domain.model.NotificacaoEnvio;
import br.com.redekrill.agendamento.domain.model.NotificacaoEnvio.Canal;
import br.com.redekrill.agendamento.domain.model.NotificacaoEnvio.Status;
import br.com.redekrill.agendamento.domain.model.Textos;
import br.com.redekrill.agendamento.domain.service.WhatsAppLink;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Avisos ao fornecedor fora do fluxo automático: e-mail da cobrança/recibo de paletes (porte de /paletes/email)
 * e WhatsApp (porte de /recebimento-whatsapp) como canal plugável — link wa.me por padrão, API opcional.
 * Toda tentativa fica registrada em notificacao_envio e na auditoria.
 */
public class NotificacaoUseCase {

    public static final String DOC_COBRANCA = "COBRANCA_PALETE";
    public static final String DOC_AGENDAMENTO = "AGENDAMENTO";

    public record Contato(String email, String telefone) {
    }

    public record MensagemWhatsApp(String telefone, String mensagem, String link, boolean apiHabilitada, String provedor) {
    }

    /** Conteúdo do e-mail de cobrança (pré-visualização e envio usam o mesmo texto). */
    public record EmailDocumento(String assunto, String html, String texto) {
    }

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PaleteFinanceiroUseCase paletes;
    private final AgendamentoUseCase agendamentos;
    private final FornecedorRepositorio fornecedores;
    private final CanalEmailDocumentos email;
    private final NotificadorWhatsApp whatsApp;
    private final NotificacaoEnvioRepositorio envios;
    private final AuditoriaUseCase auditoria;
    private final Clock clock;

    public NotificacaoUseCase(PaleteFinanceiroUseCase paletes, AgendamentoUseCase agendamentos, FornecedorRepositorio fornecedores,
                              CanalEmailDocumentos email, NotificadorWhatsApp whatsApp, NotificacaoEnvioRepositorio envios,
                              AuditoriaUseCase auditoria, Clock clock) {
        this.paletes = paletes;
        this.agendamentos = agendamentos;
        this.fornecedores = fornecedores;
        this.email = email;
        this.whatsApp = whatsApp;
        this.envios = envios;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    // ---- Contato sugerido: cadastro do fornecedor, senão o e-mail do agendamento mais recente
    public Contato contatoFornecedor(String nome) {
        Optional<Fornecedor> f = fornecedores.listar(nome, null).stream()
                .filter(x -> nome != null && (nome.equalsIgnoreCase(x.razaoSocial()) || nome.equalsIgnoreCase(x.nomeFantasia())))
                .findFirst();
        String mail = f.map(Fornecedor::email).orElse(null);
        String tel = f.map(Fornecedor::telefone).orElse(null);
        if (mail == null && nome != null && !nome.isBlank()) {
            mail = agendamentos.listar(new FiltroAgendamento(null, null, null, nome, null, null)).stream()
                    .map(AgendamentoDetalhe::agendamento)
                    .filter(a -> nome.equalsIgnoreCase(a.getFornecedor()) && a.getEmailFornecedor() != null)
                    .max(Comparator.comparing(Agendamento::getId)).map(Agendamento::getEmailFornecedor).orElse(null);
        }
        return new Contato(mail, tel);
    }

    public Contato contatoCobranca(Long cobrancaId) {
        return contatoFornecedor(paletes.cobranca(cobrancaId).getFornecedor());
    }

    // ---- E-mail da cobrança de paletes
    public EmailDocumento emailCobranca(Long cobrancaId) {
        return montarEmail(paletes.cobranca(cobrancaId));
    }

    public NotificacaoEnvio enviarEmailCobranca(Long cobrancaId, String para, Solicitante s) {
        CobrancaPalete c = paletes.cobranca(cobrancaId);
        if (c.getStatus() == CobrancaPalete.Status.CANCELADA) throw new RegraNegocioException("Cobrança cancelada não pode ser enviada");
        String destino = Textos.email(para, "E-mail do destinatário", true);
        EmailDocumento doc = montarEmail(c);
        CanalEmailDocumentos.Resultado r = email.enviar(destino, doc.assunto(), doc.html(), doc.texto());
        Status status = r.enviado() ? Status.ENVIADO : Status.FALHOU;
        return registrar(Canal.EMAIL, DOC_COBRANCA, cobrancaId, destino, doc.assunto(), email.provedor(), status, r.erro(), s);
    }

    // ---- WhatsApp
    public MensagemWhatsApp whatsAppCobranca(Long cobrancaId, String telefone) {
        CobrancaPalete c = paletes.cobranca(cobrancaId);
        String tel = telefone == null || telefone.isBlank() ? contatoFornecedor(c.getFornecedor()).telefone() : telefone;
        return mensagem(tel, textoCobranca(c));
    }

    public MensagemWhatsApp whatsAppAgendamento(Long agendamentoId, String telefone) {
        AgendamentoDetalhe d = agendamentos.buscar(agendamentoId);
        String tel = telefone;
        if ((tel == null || tel.isBlank()) && d.agendamento().getFornecedorId() != null) {
            tel = fornecedores.buscarPorId(d.agendamento().getFornecedorId()).map(Fornecedor::telefone).orElse(null);
        }
        if (tel == null || tel.isBlank()) tel = contatoFornecedor(d.agendamento().getFornecedor()).telefone();
        return mensagem(tel, textoAgendamento(d));
    }

    /**
     * Registra o uso do canal: com API habilitada envia de verdade pelo adaptador; sem API, só registra
     * que o link wa.me foi gerado (o envio é feito pelo usuário no WhatsApp).
     */
    public NotificacaoEnvio registrarWhatsApp(String documentoTipo, Long documentoId, String telefone, Solicitante s) {
        MensagemWhatsApp m = DOC_COBRANCA.equals(documentoTipo) ? whatsAppCobranca(documentoId, telefone)
                : whatsAppAgendamento(documentoId, telefone);
        String assunto = DOC_COBRANCA.equals(documentoTipo) ? "Cobrança de paletes nº " + documentoId : "Agendamento #" + documentoId;
        String destino = m.telefone() == null ? "(escolhido no WhatsApp)" : m.telefone();
        if (!whatsApp.habilitado()) {
            return registrar(Canal.WHATSAPP, documentoTipo, documentoId, destino, assunto, "wa.me", Status.LINK_GERADO, null, s);
        }
        if (m.telefone() == null) throw new RegraNegocioException("Informe um telefone válido com DDD para enviar pela API");
        NotificadorWhatsApp.Resultado r = whatsApp.enviar(m.telefone(), m.mensagem());
        return registrar(Canal.WHATSAPP, documentoTipo, documentoId, destino, assunto, whatsApp.provedor(),
                r.enviado() ? Status.ENVIADO : Status.FALHOU, r.erro(), s);
    }

    public List<NotificacaoEnvio> envios(String documentoTipo, Long documentoId) {
        return envios.listar(documentoTipo, documentoId);
    }

    private MensagemWhatsApp mensagem(String telefone, String texto) {
        String numero = WhatsAppLink.normalizarTelefone(telefone);
        return new MensagemWhatsApp(numero, texto, WhatsAppLink.link(numero, texto), whatsApp.habilitado(), whatsApp.provedor());
    }

    private NotificacaoEnvio registrar(Canal canal, String tipo, Long id, String destino, String assunto, String provedor,
                                       Status status, String erro, Solicitante s) {
        String e = erro == null ? null : erro.length() > 255 ? erro.substring(0, 255) : erro;
        NotificacaoEnvio n = envios.salvar(new NotificacaoEnvio(null, canal, tipo, id, destino,
                assunto.length() > 200 ? assunto.substring(0, 200) : assunto, provedor, status, e, s.usuario(),
                OffsetDateTime.now(clock)));
        auditoria.registrar(s, canal == Canal.EMAIL ? "ENVIAR_EMAIL" : "WHATSAPP", tipo, id,
                Map.of("destinatario", destino, "status", status.name(), "provedor", provedor));
        return n;
    }

    // ---- Textos (mesmo conteúdo no e-mail e no WhatsApp)
    static String moeda(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(v).replace('\u00a0', ' ');
    }

    static String textoCobranca(CobrancaPalete c) {
        if (c.getStatus() == CobrancaPalete.Status.PAGA) {
            return "Olá, " + c.getFornecedor() + ". Recebemos o pagamento da cobrança de paletes nº " + c.getId()
                    + " (" + c.getQuantidade() + " paletes, " + moeda(c.valorTotal()) + "). Recibo " + c.getReciboNumero()
                    + ". Agendamento Rede Krill.";
        }
        return "Olá, " + c.getFornecedor() + ". A Rede Krill registrou a cobrança de paletes nº " + c.getId() + ": "
                + c.getQuantidade() + " paletes não devolvidos × " + moeda(c.getValorUnitario()) + " = " + moeda(c.valorTotal())
                + ", vencimento " + c.getVencimento().format(BR) + ". Para quitar, devolva os paletes no CD ou combine o "
                + "pagamento com o recebimento. Agendamento Rede Krill.";
    }

    static String textoAgendamento(AgendamentoDetalhe d) {
        Agendamento a = d.agendamento();
        return "Olá, " + a.getFornecedor() + ". Agendamento #" + a.getId() + " na Rede Krill: " + a.getData().format(BR)
                + ", faixa " + d.faixaDescricao() + ", pedido " + a.getIdPedido() + ", " + a.getQuantidadePaletes()
                + " paletes (" + d.tipoCaminhao() + "). Status: " + a.getStatus().rotulo()
                + ". Apresente o ID do pedido na portaria. Para confirmar presença e informar a placa, use o link enviado por e-mail.";
    }

    static EmailDocumento montarEmail(CobrancaPalete c) {
        boolean paga = c.getStatus() == CobrancaPalete.Status.PAGA;
        String assunto = (paga ? "Recibo " + c.getReciboNumero() + " — " : "Cobrança de paletes nº " + c.getId() + " — ")
                + c.getFornecedor() + " · Agendamento Rede Krill";
        String titulo = paga ? "Recibo de paletes" : "Cobrança de paletes não devolvidos";
        String cor = paga ? "#148a42" : "#ea580c";
        StringBuilder h = new StringBuilder();
        h.append("<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#334155;max-width:560px\">")
                .append("<div style=\"background:#1e3a8a;height:6px;border-bottom:3px solid #1d4ed8\"></div>")
                .append("<img src=\"cid:logo\" alt=\"Rede Krill\" width=\"90\" height=\"83\" style=\"display:block;border:0\">")
                .append("<h2 style=\"color:").append(cor).append(";font-size:18px\">").append(titulo).append("</h2>")
                .append("<p>Olá, ").append(esc(c.getFornecedor())).append(".</p>")
                .append("<p>").append(esc(paga
                        ? "Confirmamos o recebimento referente aos paletes abaixo."
                        : "Os paletes abaixo constam como não devolvidos na conta-corrente de paletes do CD.")).append("</p>")
                .append("<table cellpadding=\"6\" style=\"border-collapse:collapse;border:1px solid #e2e8f0\">");
        linha(h, "Cobrança", "nº " + c.getId());
        linha(h, "Fornecedor", c.getFornecedor());
        linha(h, "Quantidade", c.getQuantidade() + " paletes");
        linha(h, "Valor unitário", moeda(c.getValorUnitario()));
        linha(h, "Valor total", moeda(c.valorTotal()));
        linha(h, "Vencimento", c.getVencimento().format(BR));
        if (paga) {
            linha(h, "Recibo", c.getReciboNumero());
            if (c.getFormaPagamento() != null) linha(h, "Forma", c.getFormaPagamento().name().replace('_', ' '));
        }
        if (c.getObservacao() != null) linha(h, "Observação", c.getObservacao());
        h.append("</table><p style=\"color:#64748b;font-size:12px\">")
                .append(paga ? "Guarde este e-mail como comprovante." : "Para quitar, devolva os paletes no CD ou combine o pagamento com o recebimento.")
                .append("</p><p style=\"color:#64748b;font-size:12px\">Agendamento Rede Krill</p></div>");
        return new EmailDocumento(assunto, h.toString(), textoCobranca(c));
    }

    private static void linha(StringBuilder sb, String rotulo, String valor) {
        sb.append("<tr><td style=\"background:#f1f5f9;border:1px solid #e2e8f0\"><b>").append(rotulo)
                .append("</b></td><td style=\"border:1px solid #e2e8f0\">").append(esc(valor)).append("</td></tr>");
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
