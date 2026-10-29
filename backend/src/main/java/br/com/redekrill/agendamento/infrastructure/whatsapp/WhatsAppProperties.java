package br.com.redekrill.agendamento.infrastructure.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * provedor: "desabilitado" (padrão — só link wa.me) ou "cloud-api" (WhatsApp Business Cloud API).
 * Token e ID do número vêm só de variável de ambiente.
 */
@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsAppProperties(String provedor, String apiUrl, String token, String phoneNumberId, Integer timeoutSegundos) {
    public boolean cloudApi() {
        return "cloud-api".equalsIgnoreCase(provedor) && token != null && !token.isBlank()
                && phoneNumberId != null && !phoneNumberId.isBlank();
    }
}
