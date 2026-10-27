package br.com.redekrill.agendamento.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String timezone, List<String> corsOrigins, Jwt jwt, Mail mail, Push push,
                            UsuariosIniciais usuariosIniciais) {
    public record Jwt(String secret, long expiracaoMinutos) {
    }

    public record Mail(String remetente) {
    }

    public record Push(String publicKey, String privateKey, String subject) {
    }

    public record UsuariosIniciais(String adminSenha, String usuarioSenha) {
    }
}
