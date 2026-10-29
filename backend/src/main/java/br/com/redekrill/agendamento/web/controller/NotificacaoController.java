package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.NotificacaoUseCase;
import br.com.redekrill.agendamento.domain.model.NotificacaoEnvio;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** E-mail de cobrança de paletes e canal WhatsApp (link wa.me; API opcional por ambiente). */
@RestController
@RequestMapping("/api/v1/notificacoes")
@Tag(name = "Notificações ao fornecedor", description = "E-mail da cobrança/recibo de paletes e WhatsApp")
public class NotificacaoController {
    private final NotificacaoUseCase useCase;

    public NotificacaoController(NotificacaoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/cobrancas/{id}/email")
    @Operation(summary = "Pré-visualização do e-mail e destinatário sugerido")
    public Previa previaEmail(@PathVariable Long id) {
        NotificacaoUseCase.EmailDocumento e = useCase.emailCobranca(id);
        return new Previa(e.assunto(), e.html(), e.texto(), sugerido(id));
    }

    @PostMapping("/cobrancas/{id}/email")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Envia a cobrança (ou o recibo, se paga) por e-mail e registra o envio")
    public NotificacaoEnvio enviarEmail(@PathVariable Long id, @Valid @RequestBody Requisicoes.EnvioEmail b, HttpServletRequest req) {
        return useCase.enviarEmailCobranca(id, b.para(), Requisicao.solicitante(req));
    }

    @GetMapping("/cobrancas/{id}/whatsapp")
    @Operation(summary = "Mensagem e link wa.me da cobrança")
    public NotificacaoUseCase.MensagemWhatsApp whatsAppCobranca(@PathVariable Long id, @RequestParam(required = false) String telefone) {
        return useCase.whatsAppCobranca(id, telefone);
    }

    @GetMapping("/agendamentos/{id}/whatsapp")
    @Operation(summary = "Mensagem e link wa.me de confirmação do agendamento")
    public NotificacaoUseCase.MensagemWhatsApp whatsAppAgendamento(@PathVariable Long id, @RequestParam(required = false) String telefone) {
        return useCase.whatsAppAgendamento(id, telefone);
    }

    @PostMapping("/{documento}/{id}/whatsapp")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra o link aberto (ou envia pela API, se configurada). documento: cobrancas | agendamentos")
    public NotificacaoEnvio whatsApp(@PathVariable String documento, @PathVariable Long id,
                                     @Valid @RequestBody(required = false) Requisicoes.EnvioWhatsApp b, HttpServletRequest req) {
        return useCase.registrarWhatsApp(tipo(documento), id, b == null ? null : b.telefone(), Requisicao.solicitante(req));
    }

    @GetMapping("/{documento}/{id}/envios")
    @Operation(summary = "Histórico de envios do documento")
    public List<NotificacaoEnvio> envios(@PathVariable String documento, @PathVariable Long id) {
        return useCase.envios(tipo(documento), id);
    }

    public record Previa(String assunto, String html, String texto, NotificacaoUseCase.Contato contato) {
    }

    private NotificacaoUseCase.Contato sugerido(Long cobrancaId) {
        return useCase.contatoCobranca(cobrancaId);
    }

    private static String tipo(String documento) {
        return switch (documento) {
            case "cobrancas" -> NotificacaoUseCase.DOC_COBRANCA;
            case "agendamentos" -> NotificacaoUseCase.DOC_AGENDAMENTO;
            default -> throw new br.com.redekrill.agendamento.domain.exception.RegraNegocioException("Documento inválido: " + documento);
        };
    }
}
