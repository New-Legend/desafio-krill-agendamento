package br.com.redekrill.agendamento.infrastructure.security;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.usecase.AuditoriaUseCase;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.UsuarioEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.UsuarioJpa;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AutenticacaoService {

    /** Hash para gastar o mesmo tempo quando o usuário não existe (evita enumeração por tempo). */
    private final String hashFalso;

    private final UsuarioJpa usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditoriaUseCase auditoria;

    public AutenticacaoService(UsuarioJpa usuarios, PasswordEncoder encoder, JwtService jwt, AuditoriaUseCase auditoria) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.auditoria = auditoria;
        this.hashFalso = encoder.encode("usuario-inexistente");
    }

    public Sessao autenticar(String username, String senha, String ip) {
        String login = username == null ? "" : username.trim().toLowerCase();
        UsuarioEntity u = usuarios.findByUsernameIgnoreCase(login).filter(x -> x.ativo).orElse(null);
        boolean ok = encoder.matches(senha == null ? "" : senha, u == null ? hashFalso : u.senhaHash) && u != null;
        if (!ok) {
            auditoria.registrar(new Solicitante(login.isEmpty() ? "anonimo" : login, ip), "LOGIN_FALHA", "SESSAO",
                    null, false, Map.of("motivo", "Credenciais inválidas"));
            throw new BadCredentialsException("Usuário ou senha inválidos");
        }
        auditoria.registrar(new Solicitante(u.username, ip), "LOGIN", "SESSAO", u.id, Map.of("perfil", u.perfil.name()));
        return new Sessao(jwt.gerar(u.username, u.nome, u.perfil.name()), u.username, u.nome, u.perfil.name(),
                jwt.expiracaoSegundos());
    }

    public record Sessao(String token, String usuario, String nome, String perfil, long expiraEmSegundos) {
    }
}
