package br.com.redekrill.agendamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync(proxyTargetClass = true)
@ConfigurationPropertiesScan
public class AgendamentoApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        SpringApplication.run(AgendamentoApplication.class, args);
    }
}
