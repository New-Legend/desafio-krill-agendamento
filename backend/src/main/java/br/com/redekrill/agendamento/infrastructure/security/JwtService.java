package br.com.redekrill.agendamento.infrastructure.security;

import br.com.redekrill.agendamento.infrastructure.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(AppProperties props) {
        byte[] bytes = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 bytes");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMinutos = props.jwt().expiracaoMinutos();
    }

    public String gerar(String usuario, String nome, String perfil) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario)
                .claim("nome", nome)
                .claim("perfil", perfil)
                .issuer("agendamento-api")
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoMinutos * 60)))
                .signWith(chave)
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser().verifyWith(chave).requireIssuer("agendamento-api").build()
                .parseSignedClaims(token).getPayload();
    }

    public long expiracaoSegundos() {
        return expiracaoMinutos * 60;
    }
}
