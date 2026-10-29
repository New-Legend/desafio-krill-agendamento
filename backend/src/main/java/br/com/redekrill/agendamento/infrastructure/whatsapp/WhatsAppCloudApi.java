package br.com.redekrill.agendamento.infrastructure.whatsapp;

import br.com.redekrill.agendamento.application.port.NotificadorWhatsApp;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Adaptador para a API do WhatsApp Business (Cloud API): POST {apiUrl}/{phoneNumberId}/messages.
 * Só é ligado com WHATSAPP_PROVEDOR=cloud-api + token + ID do número. Observação: fora da janela de
 * 24 h a Meta exige template aprovado; aqui vai mensagem de texto, suficiente para o fluxo do teste.
 */
public class WhatsAppCloudApi implements NotificadorWhatsApp {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudApi.class);
    private final WhatsAppProperties props;
    private final HttpClient http;
    private final ObjectMapper json;

    public WhatsAppCloudApi(WhatsAppProperties props, HttpClient http, ObjectMapper json) {
        this.props = props;
        this.http = http;
        this.json = json;
    }

    @Override
    public boolean habilitado() {
        return true;
    }

    @Override
    public String provedor() {
        return "cloud-api";
    }

    @Override
    public Resultado enviar(String telefone, String mensagem) {
        try {
            String base = props.apiUrl() == null || props.apiUrl().isBlank() ? "https://graph.facebook.com/v20.0" : props.apiUrl();
            String corpo = json.writeValueAsString(Map.of("messaging_product", "whatsapp", "to", telefone, "type", "text",
                    "text", Map.of("preview_url", false, "body", mensagem)));
            HttpRequest req = HttpRequest.newBuilder(URI.create(base.replaceAll("/+$", "") + "/" + props.phoneNumberId() + "/messages"))
                    .timeout(Duration.ofSeconds(props.timeoutSegundos() == null ? 10 : props.timeoutSegundos()))
                    .header("Authorization", "Bearer " + props.token())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpo)).build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 == 2) return new Resultado(true, null);
            log.warn("WhatsApp API respondeu {}", resp.statusCode());
            return new Resultado(false, "WhatsApp API respondeu HTTP " + resp.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Resultado(false, "Envio interrompido");
        } catch (Exception e) {
            log.warn("Falha no envio pelo WhatsApp: {}", e.getMessage());
            return new Resultado(false, "Falha no envio: " + e.getClass().getSimpleName());
        }
    }
}
