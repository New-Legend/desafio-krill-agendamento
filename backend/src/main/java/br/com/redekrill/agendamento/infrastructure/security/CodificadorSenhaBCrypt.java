package br.com.redekrill.agendamento.infrastructure.security;

import br.com.redekrill.agendamento.application.port.CodificadorSenha;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CodificadorSenhaBCrypt implements CodificadorSenha {
    private final PasswordEncoder encoder;

    public CodificadorSenhaBCrypt(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String codificar(String senha) {
        return encoder.encode(senha);
    }

    @Override
    public boolean confere(String senha, String hash) {
        return hash != null && encoder.matches(senha, hash);
    }
}
