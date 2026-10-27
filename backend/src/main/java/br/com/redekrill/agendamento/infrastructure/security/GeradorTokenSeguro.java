package br.com.redekrill.agendamento.infrastructure.security;

import br.com.redekrill.agendamento.application.port.GeradorToken;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Token de 256 bits (SecureRandom) em base64url; no banco fica apenas o SHA-256. */
@Component
public class GeradorTokenSeguro implements GeradorToken {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String novo() {
        byte[] b = new byte[32];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    @Override
    public String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
