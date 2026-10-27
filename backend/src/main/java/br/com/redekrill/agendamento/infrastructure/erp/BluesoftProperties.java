package br.com.redekrill.agendamento.infrastructure.erp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração do Bluesoft ERP (tudo via variáveis de ambiente; nada versionado). */
@ConfigurationProperties(prefix = "app.erp.bluesoft")
public record BluesoftProperties(String baseUrl, String tenant, String clientId, String clientSecret, Integer timeoutSegundos) {
    public String urlTenant() {
        String base = baseUrl == null || baseUrl.isBlank() ? "https://erp.bluesoft.com.br" : baseUrl;
        return base.replaceAll("/+$", "") + "/" + tenant;
    }
}
