package br.com.redekrill.agendamento.infrastructure.security;

import br.com.redekrill.agendamento.domain.model.Perfil;
import br.com.redekrill.agendamento.infrastructure.config.AppProperties;
import br.com.redekrill.agendamento.infrastructure.persistence.entity.UsuarioEntity;
import br.com.redekrill.agendamento.infrastructure.persistence.jpa.UsuarioJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria os usuários padrão (admin / usuario) na primeira inicialização. */
@Component
public class UsuariosIniciais implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuariosIniciais.class);

    private final UsuarioJpa usuarios;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public UsuariosIniciais(UsuarioJpa usuarios, PasswordEncoder encoder, AppProperties props) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        criar("admin", "Administrador", props.usuariosIniciais().adminSenha(), Perfil.ADMIN);
        criar("usuario", "Usuário Comum", props.usuariosIniciais().usuarioSenha(), Perfil.USUARIO);
    }

    private void criar(String username, String nome, String senha, Perfil perfil) {
        if (usuarios.findByUsernameIgnoreCase(username).isPresent()) return;
        UsuarioEntity u = new UsuarioEntity();
        u.username = username;
        u.nome = nome;
        u.senhaHash = encoder.encode(senha);
        u.perfil = perfil;
        u.ativo = true;
        usuarios.save(u);
        log.info("Usuário inicial '{}' criado com perfil {}", username, perfil);
    }
}
