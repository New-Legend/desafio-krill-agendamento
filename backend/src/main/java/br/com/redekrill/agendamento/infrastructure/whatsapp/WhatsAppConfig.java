package br.com.redekrill.agendamento.infrastructure.whatsapp;

import br.com.redekrill.agendamento.application.port.NotificadorWhatsApp;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/** Escolhe o adaptador do canal WhatsApp pelo ambiente (desabilitado por padrão). */
@Configuration
public class WhatsAppConfig {
    @Bean
    NotificadorWhatsApp notificadorWhatsApp(WhatsAppProperties props, ObjectMapper json) {
        if (props.cloudApi()) {
            LoggerFactory.getLogger(WhatsAppConfig.class).info("WhatsApp: envio pela Cloud API habilitado");
            return new WhatsAppCloudApi(props, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), json);
        }
        return new WhatsAppDesabilitado();
    }
}
