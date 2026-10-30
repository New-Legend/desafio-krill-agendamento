package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.CodificadorSenha;
import br.com.redekrill.agendamento.application.port.LogAcessoRepositorio;
import br.com.redekrill.agendamento.application.port.PerfilAcessoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.application.port.UsuarioRepositorio;
import br.com.redekrill.agendamento.domain.exception.CredenciaisInvalidasException;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.EventoAcesso;
import br.com.redekrill.agendamento.domain.model.LogAcesso;
import br.com.redekrill.agendamento.domain.model.PerfilAcesso;
import br.com.redekrill.agendamento.domain.model.Permissao;
import br.com.redekrill.agendamento.domain.model.UsuarioSistema;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Usuários, perfis com permissões numeradas, login/logout/troca de senha e log de acessos. */
public class AcessoUseCase {

    public record Contexto(UsuarioSistema usuario, PerfilAcesso perfil) {
        public Set<Integer> permissoes() {
            return perfil.ativo() ? perfil.permissoes() : Set.of();
        }
    }

    public record DadosUsuario(String login, String nome, Long perfilId, Boolean ativo, String senhaProvisoria) {
    }

    public record DadosPerfil(String nome, String descricao, Boolean ativo, Set<Integer> permissoes) {
    }

    public record Origem(String ip, String navegador) {
    }

    private final UsuarioRepositorio usuarios;
    private final PerfilAcessoRepositorio perfis;
    private final LogAcessoRepositorio logs;
    private final CodificadorSenha senhas;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;
    private final Clock clock;
    /** Hash para gastar o mesmo tempo quando o login não existe (evita enumeração por tempo). */
    private final String hashFalso;

    public AcessoUseCase(UsuarioRepositorio usuarios, PerfilAcessoRepositorio perfis, LogAcessoRepositorio logs,
                         CodificadorSenha senhas, AuditoriaUseCase auditoria, Transacao transacao, Clock clock) {
        this.usuarios = usuarios;
        this.perfis = perfis;
        this.logs = logs;
        this.senhas = senhas;
        this.auditoria = auditoria;
        this.transacao = transacao;
        this.clock = clock;
        this.hashFalso = senhas.codificar("login-inexistente-1");
    }

    // ---------- sessão ----------

    public Contexto autenticar(String login, String senha, Origem o) {
        String l = login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
        UsuarioSistema u = usuarios.buscarPorLogin(l).filter(UsuarioSistema::ativo).orElse(null);
        boolean ok = senhas.confere(senha == null ? "" : senha, u == null ? hashFalso : u.senhaHash()) && u != null;
        if (!ok) {
            logs.registrar(l.isEmpty() ? "anonimo" : l, u == null ? null : u.id(), EventoAcesso.SENHA_RECUSADA, o.ip(), o.navegador());
            auditoria.registrar(new Solicitante(l.isEmpty() ? "anonimo" : l, o.ip()), "LOGIN_FALHA", "SESSAO", null, false,
                    Map.of("motivo", "Credenciais inválidas"));
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos");
        }
        usuarios.registrarAcesso(u.id(), Instant.now(clock));
        logs.registrar(u.login(), u.id(), EventoAcesso.ENTRADA, o.ip(), o.navegador());
        auditoria.registrar(new Solicitante(u.login(), o.ip()), "LOGIN", "SESSAO", u.id(), Map.of("perfil", u.perfilNome()));
        return contexto(u);
    }

    public void sair(String login, Origem o) {
        usuarios.buscarPorLogin(login).ifPresent(u -> logs.registrar(u.login(), u.id(), EventoAcesso.SAIDA, o.ip(), o.navegador()));
    }

    /** Contexto atual do login (null se inativo ou inexistente) — usado a cada requisição. */
    public Optional<Contexto> contextoAtual(String login) {
        return usuarios.buscarPorLogin(login).filter(UsuarioSistema::ativo).map(this::contexto);
    }

    public void trocarSenha(String login, String atual, String nova, Origem o) {
        transacao.executar(() -> {
            UsuarioSistema u = usuarios.buscarPorLogin(login).filter(UsuarioSistema::ativo)
                    .orElseThrow(() -> new CredenciaisInvalidasException("Sessão inválida"));
            if (!senhas.confere(atual == null ? "" : atual, u.senhaHash())) throw new RegraNegocioException("Senha atual não confere");
            UsuarioSistema.validarSenha(nova, u.login());
            if (senhas.confere(nova, u.senhaHash())) throw new RegraNegocioException("A nova senha deve ser diferente da atual");
            usuarios.salvar(comSenha(u, senhas.codificar(nova), false));
            logs.registrar(u.login(), u.id(), EventoAcesso.SENHA_TROCADA, o.ip(), o.navegador());
            return null;
        });
    }

    private Contexto contexto(UsuarioSistema u) {
        PerfilAcesso p = perfis.buscarPorId(u.perfilId()).orElseThrow(() -> new RecursoNaoEncontradoException("Perfil", u.perfilId()));
        return new Contexto(u, p);
    }

    // ---------- usuários ----------

    public List<UsuarioSistema> listarUsuarios(String texto, Long perfilId, Boolean ativo) {
        String t = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return usuarios.listar().stream()
                .filter(u -> t.isEmpty() || u.login().contains(t) || u.nome().toLowerCase(Locale.ROOT).contains(t))
                .filter(u -> perfilId == null || perfilId.equals(u.perfilId()))
                .filter(u -> ativo == null || ativo == u.ativo())
                .toList();
    }

    public UsuarioSistema salvarUsuario(Long id, DadosUsuario d, Solicitante s) {
        return transacao.executar(() -> {
            PerfilAcesso perfil = d.perfilId() == null ? null : perfis.buscarPorId(d.perfilId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil", d.perfilId()));
            if (perfil != null && !perfil.ativo()) throw new RegraNegocioException("Perfil inativo não pode receber usuários");
            boolean ativo = d.ativo() == null || d.ativo();
            UsuarioSistema salvo;
            if (id == null) {
                UsuarioSistema novo = new UsuarioSistema(null, d.login(), d.nome(), d.perfilId(), null, ativo, true, null, null, null);
                if (usuarios.buscarPorLogin(novo.login()).isPresent()) throw new RegraNegocioException("Já existe um usuário com este login");
                UsuarioSistema.validarSenha(d.senhaProvisoria(), novo.login());
                salvo = usuarios.salvar(comSenha(novo, senhas.codificar(d.senhaProvisoria()), true));
            } else {
                UsuarioSistema atual = usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
                if (atual.login().equals(s.usuario()) && (!ativo || !atual.perfilId().equals(d.perfilId())))
                    throw new RegraNegocioException("Você não pode inativar nem trocar o perfil do seu próprio usuário");
                salvo = usuarios.salvar(new UsuarioSistema(id, atual.login(), d.nome(), d.perfilId(), null, ativo,
                        atual.trocarSenha(), atual.ultimoAcesso(), atual.criadoEm(), atual.senhaHash()));
            }
            auditoria.registrar(s, id == null ? "CRIAR" : "ALTERAR", "USUARIO", salvo.id(),
                    Map.of("login", salvo.login(), "perfil", salvo.perfilNome() == null ? "" : salvo.perfilNome(), "ativo", salvo.ativo()));
            return salvo;
        });
    }

    /** Redefine a senha com uma provisória; o usuário troca no próximo acesso. */
    public void redefinirSenha(Long id, String senhaProvisoria, Solicitante s) {
        transacao.executar(() -> {
            UsuarioSistema u = usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
            UsuarioSistema.validarSenha(senhaProvisoria, u.login());
            usuarios.salvar(comSenha(u, senhas.codificar(senhaProvisoria), true));
            auditoria.registrar(s, "REDEFINIR_SENHA", "USUARIO", id, Map.of("login", u.login()));
            return null;
        });
    }

    private static UsuarioSistema comSenha(UsuarioSistema u, String hash, boolean trocar) {
        return new UsuarioSistema(u.id(), u.login(), u.nome(), u.perfilId(), u.perfilNome(), u.ativo(), trocar,
                u.ultimoAcesso(), u.criadoEm(), hash);
    }

    // ---------- perfis ----------

    public List<PerfilAcesso> listarPerfis() {
        return perfis.listar();
    }

    public PerfilAcesso salvarPerfil(Long id, DadosPerfil d, Solicitante s) {
        return transacao.executar(() -> {
            PerfilAcesso atual = id == null ? null : perfis.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Perfil", id));
            PerfilAcesso p = new PerfilAcesso(id, d.nome(), d.descricao(), d.ativo() == null || d.ativo(), d.permissoes(),
                    atual == null ? 0 : atual.usuarios());
            perfis.buscarPorNome(p.nome()).filter(x -> !x.id().equals(id))
                    .ifPresent(x -> { throw new RegraNegocioException("Já existe um perfil com este nome"); });
            if (atual != null && !p.ativo() && atual.usuarios() > 0)
                throw new RegraNegocioException("Perfil com usuários vinculados não pode ser inativado");
            usuarios.buscarPorLogin(s.usuario()).filter(u -> u.perfilId().equals(id)).ifPresent(u -> {
                if (!p.concede(Permissao.PERFIS) || !p.concede(Permissao.USUARIOS_EDITAR))
                    throw new RegraNegocioException("Seu próprio perfil precisa manter as permissões 1002 e 1003");
            });
            PerfilAcesso salvo = perfis.salvar(p);
            auditoria.registrar(s, id == null ? "CRIAR" : "ALTERAR", "PERFIL_ACESSO", salvo.id(),
                    Map.of("nome", salvo.nome(), "permissoes", salvo.permissoes().stream().sorted().toList(), "ativo", salvo.ativo()));
            return salvo;
        });
    }

    // ---------- logs ----------

    public List<LogAcesso> logs(String texto, EventoAcesso evento, int dias, int limite) {
        int d = Math.max(1, Math.min(90, dias));
        int l = Math.max(50, Math.min(2000, limite));
        return logs.buscar(texto == null || texto.isBlank() ? null : texto.trim(), evento,
                Instant.now(clock).minus(Duration.ofDays(d)), l);
    }
}
