package br.com.redekrill.agendamento.infrastructure.security;

import br.com.redekrill.agendamento.application.port.CodificadorSenha;
import br.com.redekrill.agendamento.application.port.PerfilAcessoRepositorio;
import br.com.redekrill.agendamento.application.port.UsuarioRepositorio;
import br.com.redekrill.agendamento.domain.model.UsuarioSistema;
import br.com.redekrill.agendamento.infrastructure.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Cria os usuários padrão (admin no perfil Administrador, usuario no perfil Consulta) na primeira inicialização. */
@Component
public class UsuariosIniciais implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuariosIniciais.class);

    private final UsuarioRepositorio usuarios;
    private final PerfilAcessoRepositorio perfis;
    private final CodificadorSenha senhas;
    private final AppProperties props;

    public UsuariosIniciais(UsuarioRepositorio usuarios, PerfilAcessoRepositorio perfis, CodificadorSenha senhas, AppProperties props) {
        this.usuarios = usuarios;
        this.perfis = perfis;
        this.senhas = senhas;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        criar("admin", "Administrador", props.usuariosIniciais().adminSenha(), "Administrador");
        criar("usuario", "Usuário Comum", props.usuariosIniciais().usuarioSenha(), "Consulta");
    }

    private void criar(String login, String nome, String senha, String perfil) {
        if (usuarios.buscarPorLogin(login).isPresent()) return;
        Long perfilId = perfis.buscarPorNome(perfil).orElseThrow().id();
        usuarios.salvar(new UsuarioSistema(null, login, nome, perfilId, null, true, false, null, null, senhas.codificar(senha)));
        log.info("Usuário inicial '{}' criado com perfil {}", login, perfil);
    }
}
