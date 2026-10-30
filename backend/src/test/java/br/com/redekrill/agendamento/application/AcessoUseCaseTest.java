package br.com.redekrill.agendamento.application;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AuditoriaRepositorio;
import br.com.redekrill.agendamento.application.port.CodificadorSenha;
import br.com.redekrill.agendamento.application.port.LogAcessoRepositorio;
import br.com.redekrill.agendamento.application.port.PerfilAcessoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.application.port.UsuarioRepositorio;
import br.com.redekrill.agendamento.application.usecase.AcessoUseCase;
import br.com.redekrill.agendamento.application.usecase.AuditoriaUseCase;
import br.com.redekrill.agendamento.domain.exception.CredenciaisInvalidasException;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.EventoAcesso;
import br.com.redekrill.agendamento.domain.model.LogAcesso;
import br.com.redekrill.agendamento.domain.model.PerfilAcesso;
import br.com.redekrill.agendamento.domain.model.Permissao;
import br.com.redekrill.agendamento.domain.model.UsuarioSistema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AcessoUseCaseTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T14:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    static final Transacao TX = new Transacao() {
        @Override
        public <T> T executar(java.util.function.Supplier<T> op) {
            return op.get();
        }
    };
    static final AcessoUseCase.Origem ORIGEM = new AcessoUseCase.Origem("10.0.0.1", "Mozilla/5.0 (Windows NT 10.0) Chrome/130");
    static final Set<Integer> TODAS = Arrays.stream(Permissao.values()).map(Permissao::codigo).collect(Collectors.toSet());

    final Map<Long, UsuarioSistema> us = new HashMap<>();
    final Map<Long, PerfilAcesso> ps = new HashMap<>();
    final List<String[]> eventos = new ArrayList<>();
    AcessoUseCase uc;
    final Solicitante admin = new Solicitante("admin", "10.0.0.1");

    final CodificadorSenha cod = new CodificadorSenha() {
        public String codificar(String s) { return "h:" + s; }
        public boolean confere(String s, String h) { return h != null && h.equals("h:" + s); }
    };

    final UsuarioRepositorio usuarios = new UsuarioRepositorio() {
        public List<UsuarioSistema> listar() { return new ArrayList<>(us.values()); }
        public Optional<UsuarioSistema> buscarPorId(Long id) { return Optional.ofNullable(us.get(id)); }
        public Optional<UsuarioSistema> buscarPorLogin(String l) { return us.values().stream().filter(u -> u.login().equalsIgnoreCase(l)).findFirst(); }
        public UsuarioSistema salvar(UsuarioSistema u) {
            Long id = u.id() == null ? us.size() + 1L : u.id();
            var s = new UsuarioSistema(id, u.login(), u.nome(), u.perfilId(), ps.get(u.perfilId()).nome(), u.ativo(), u.trocarSenha(),
                    u.ultimoAcesso(), u.criadoEm(), u.senhaHash());
            us.put(id, s);
            return s;
        }
        public void registrarAcesso(Long id, Instant q) {
            var u = us.get(id);
            us.put(id, new UsuarioSistema(id, u.login(), u.nome(), u.perfilId(), u.perfilNome(), u.ativo(), u.trocarSenha(), q, u.criadoEm(), u.senhaHash()));
        }
    };

    final PerfilAcessoRepositorio perfis = new PerfilAcessoRepositorio() {
        public List<PerfilAcesso> listar() { return new ArrayList<>(ps.values()); }
        public Optional<PerfilAcesso> buscarPorId(Long id) { return Optional.ofNullable(ps.get(id)); }
        public Optional<PerfilAcesso> buscarPorNome(String n) { return ps.values().stream().filter(p -> p.nome().equalsIgnoreCase(n)).findFirst(); }
        public PerfilAcesso salvar(PerfilAcesso p) {
            Long id = p.id() == null ? ps.size() + 1L : p.id();
            var s = new PerfilAcesso(id, p.nome(), p.descricao(), p.ativo(), p.permissoes(), p.usuarios());
            ps.put(id, s);
            return s;
        }
    };

    final LogAcessoRepositorio logs = new LogAcessoRepositorio() {
        public void registrar(String u, Long id, EventoAcesso e, String ip, String nav) { eventos.add(new String[]{u, e.name()}); }
        public List<LogAcesso> buscar(String t, EventoAcesso e, Instant d, int l) {
            return eventos.stream().filter(x -> e == null || x[1].equals(e.name()))
                    .map(x -> new LogAcesso(1L, x[0], EventoAcesso.valueOf(x[1]), "10.0.0.1", null, d)).limit(l).toList();
        }
    };

    @BeforeEach
    void setUp() {
        uc = new AcessoUseCase(usuarios, perfis, logs, cod, new AuditoriaUseCase(mock(AuditoriaRepositorio.class), CLOCK), TX, CLOCK);
        perfis.salvar(new PerfilAcesso(null, "Administrador", "Todas as telas", true, TODAS, 1));
        perfis.salvar(new PerfilAcesso(null, "Portaria", null, true, Set.of(1101, 1102, 1204), 0));
        usuarios.salvar(new UsuarioSistema(null, "admin", "Administrador", 1L, null, true, false, null, null, "h:admin123"));
    }

    @Test
    void loginRegistraEntradaUltimoAcessoEPermissoes() {
        var ctx = uc.autenticar(" ADMIN ", "admin123", ORIGEM);
        assertThat(ctx.permissoes()).containsAll(TODAS);
        assertThat(us.get(1L).ultimoAcesso()).isEqualTo(Instant.now(CLOCK));
        assertThat(eventos).extracting(e -> e[1]).containsExactly("ENTRADA");
        uc.sair("admin", ORIGEM);
        assertThat(eventos).extracting(e -> e[1]).containsExactly("ENTRADA", "SAIDA");
    }

    @Test
    void senhaErradaOuUsuarioInativoSaoRecusados() {
        assertThatThrownBy(() -> uc.autenticar("admin", "errada", ORIGEM)).isInstanceOf(CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> uc.autenticar("", null, ORIGEM)).isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(eventos).extracting(e -> e[0] + ":" + e[1]).containsExactly("admin:SENHA_RECUSADA", "anonimo:SENHA_RECUSADA");
        assertThat(uc.logs(null, EventoAcesso.SENHA_RECUSADA, 500, 1)).hasSize(2);
        var u = uc.salvarUsuario(null, new AcessoUseCase.DadosUsuario("joao.silva", "João Silva", 2L, false, "Portaria2026"), admin);
        assertThatThrownBy(() -> uc.autenticar("joao.silva", "Portaria2026", ORIGEM)).isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(uc.contextoAtual(u.login())).isEmpty();
    }

    @Test
    void novoUsuarioTrocaSenhaNoPrimeiroAcesso() {
        var u = uc.salvarUsuario(null, new AcessoUseCase.DadosUsuario("Maria.Souza", "Maria  Souza", 2L, null, "Provisoria1"), admin);
        assertThat(u.login()).isEqualTo("maria.souza");
        assertThat(u.nome()).isEqualTo("Maria Souza");
        assertThat(u.trocarSenha()).isTrue();
        var ctx = uc.autenticar("maria.souza", "Provisoria1", ORIGEM);
        assertThat(ctx.usuario().trocarSenha()).isTrue();
        assertThat(ctx.permissoes()).containsExactlyInAnyOrder(1101, 1102, 1204);
        assertThatThrownBy(() -> uc.trocarSenha("maria.souza", "outra", "NovaSenha9", ORIGEM)).hasMessage("Senha atual não confere");
        assertThatThrownBy(() -> uc.trocarSenha("maria.souza", "Provisoria1", "Provisoria1", ORIGEM)).hasMessageContaining("diferente");
        assertThatThrownBy(() -> uc.trocarSenha("ninguem", "x", "y", ORIGEM)).isInstanceOf(CredenciaisInvalidasException.class);
        uc.trocarSenha("maria.souza", "Provisoria1", "NovaSenha9", ORIGEM);
        assertThat(uc.contextoAtual("maria.souza").orElseThrow().usuario().trocarSenha()).isFalse();
        assertThat(eventos).extracting(e -> e[1]).contains("SENHA_TROCADA");
        uc.redefinirSenha(u.id(), "Redefinida7", admin);
        assertThat(uc.contextoAtual("maria.souza").orElseThrow().usuario().trocarSenha()).isTrue();
        assertThatThrownBy(() -> uc.redefinirSenha(99L, "Redefinida7", admin)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void regrasDeUsuario() {
        var dados = new AcessoUseCase.DadosUsuario("ana.lima", "Ana Lima", 2L, true, "Portaria2026");
        uc.salvarUsuario(null, dados, admin);
        assertThatThrownBy(() -> uc.salvarUsuario(null, dados, admin)).hasMessage("Já existe um usuário com este login");
        assertThatThrownBy(() -> uc.salvarUsuario(null, new AcessoUseCase.DadosUsuario("x", "X Y", 2L, true, "Portaria2026"), admin))
                .hasMessageContaining("Login deve ter");
        assertThatThrownBy(() -> uc.salvarUsuario(null, new AcessoUseCase.DadosUsuario("bruno", "Bruno", 2L, true, "curta1"), admin))
                .hasMessageContaining("8 caracteres");
        assertThatThrownBy(() -> uc.salvarUsuario(null, new AcessoUseCase.DadosUsuario("bruno", "Bruno", 2L, true, "somenteletras"), admin))
                .hasMessage("A senha deve ter letras e números");
        assertThatThrownBy(() -> uc.salvarUsuario(null, new AcessoUseCase.DadosUsuario("bruno123", "Bruno", 2L, true, "BRUNO123"), admin))
                .hasMessage("A senha não pode ser igual ao login");
        assertThatThrownBy(() -> uc.salvarUsuario(null, new AcessoUseCase.DadosUsuario("bruno", "Bruno", 9L, true, "Portaria2026"), admin))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> uc.salvarUsuario(null, new AcessoUseCase.DadosUsuario("bruno", "Bruno", null, true, "Portaria2026"), admin))
                .hasMessage("Perfil é obrigatório");
        assertThatThrownBy(() -> uc.salvarUsuario(1L, new AcessoUseCase.DadosUsuario(null, "Administrador", 1L, false, null), admin))
                .hasMessageContaining("próprio usuário");
        assertThatThrownBy(() -> uc.salvarUsuario(1L, new AcessoUseCase.DadosUsuario(null, "Administrador", 2L, true, null), admin))
                .hasMessageContaining("próprio usuário");
        assertThatThrownBy(() -> uc.salvarUsuario(50L, dados, admin)).isInstanceOf(RecursoNaoEncontradoException.class);
        var alterado = uc.salvarUsuario(2L, new AcessoUseCase.DadosUsuario("ignorado", "Ana Lima Costa", 1L, false, null), admin);
        assertThat(alterado.login()).isEqualTo("ana.lima");
        assertThat(alterado.ativo()).isFalse();
        assertThat(uc.listarUsuarios("costa", null, null)).hasSize(1);
        assertThat(uc.listarUsuarios(null, 1L, true)).extracting(UsuarioSistema::login).containsExactly("admin");
        assertThat(uc.listarUsuarios("  ", null, false)).hasSize(1);
        perfis.salvar(new PerfilAcesso(3L, "Antigo", null, false, Set.of(), 0));
        assertThatThrownBy(() -> uc.salvarUsuario(null, new AcessoUseCase.DadosUsuario("carla", "Carla", 3L, true, "Portaria2026"), admin))
                .hasMessage("Perfil inativo não pode receber usuários");
    }

    @Test
    void perfisValidamNumerosEProtegemOProprioAcesso() {
        var p = uc.salvarPerfil(null, new AcessoUseCase.DadosPerfil("Conferência", "Conferentes do turno", null, Set.of(1204, 1102)), admin);
        assertThat(p.permissoes()).containsExactlyInAnyOrder(1102, 1204);
        assertThat(p.concede(Permissao.PORTARIA_CONFERENCIA)).isTrue();
        assertThat(p.concede(Permissao.AUDITORIA)).isFalse();
        assertThatThrownBy(() -> uc.salvarPerfil(null, new AcessoUseCase.DadosPerfil("conferência", null, true, Set.of()), admin))
                .hasMessage("Já existe um perfil com este nome");
        assertThatThrownBy(() -> uc.salvarPerfil(null, new AcessoUseCase.DadosPerfil("Outro", null, true, Set.of(999)), admin))
                .hasMessage("Permissão desconhecida: 999");
        assertThatThrownBy(() -> uc.salvarPerfil(null, new AcessoUseCase.DadosPerfil("AB", null, true, Set.of()), admin))
                .hasMessageContaining("ao menos 3");
        assertThatThrownBy(() -> uc.salvarPerfil(1L, new AcessoUseCase.DadosPerfil("Administrador", null, true, Set.of(1001)), admin))
                .hasMessageContaining("1002 e 1003");
        assertThatThrownBy(() -> uc.salvarPerfil(1L, new AcessoUseCase.DadosPerfil("Administrador", null, false, TODAS), admin))
                .hasMessageContaining("usuários vinculados");
        assertThatThrownBy(() -> uc.salvarPerfil(77L, new AcessoUseCase.DadosPerfil("Novo", null, true, Set.of()), admin))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        var inativo = uc.salvarPerfil(p.id(), new AcessoUseCase.DadosPerfil("Conferência", null, false, Set.of(1102)), admin);
        assertThat(inativo.concede(Permissao.AGENDAMENTOS_VER)).isFalse();
        assertThat(uc.listarPerfis()).hasSize(3);
        assertThat(new AcessoUseCase.Contexto(us.get(1L), inativo).permissoes()).isEmpty();
    }

    @Test
    void catalogoNumeradoSemRepeticao() {
        assertThat(Arrays.stream(Permissao.values()).map(Permissao::codigo).distinct().count()).isEqualTo(Permissao.values().length);
        assertThat(Permissao.values()[0].codigo()).isEqualTo(1001);
        assertThat(Permissao.porCodigo(1103)).contains(Permissao.AGENDAMENTO_CRIAR);
        assertThat(Permissao.AGENDAMENTO_CRIAR.authority()).isEqualTo("PERM_1103");
        assertThat(Permissao.AGENDAMENTO_CRIAR.modulo()).isEqualTo("Operação");
        assertThat(Permissao.AGENDAMENTO_CRIAR.nome()).isEqualTo("Agendamentos: incluir");
        assertThat(Permissao.AGENDAMENTO_CRIAR.telas()).contains("novo");
        assertThat(EventoAcesso.SAIDA.rotulo()).isEqualTo("Saiu do sistema");
        assertThat(LogAcesso.resumoNavegador(ORIGEM.navegador())).isEqualTo("Chrome · Windows");
        assertThat(LogAcesso.resumoNavegador("Mozilla/5.0 (iPhone) Safari/605")).isEqualTo("Safari · iOS");
        assertThat(LogAcesso.resumoNavegador("Mozilla/5.0 (Linux; Android 14) Firefox/130")).isEqualTo("Firefox · Android");
        assertThat(LogAcesso.resumoNavegador("Mozilla/5.0 (Macintosh; Mac OS X) Edg/130")).isEqualTo("Edge · macOS");
        assertThat(LogAcesso.resumoNavegador("curl/8 (X11; Linux) OPR/1")).isEqualTo("Opera · Linux");
        assertThat(LogAcesso.resumoNavegador("curl/8")).isEqualTo("Outro");
        assertThat(LogAcesso.resumoNavegador(null)).isEqualTo("—");
        assertThatThrownBy(() -> UsuarioSistema.validarSenha("a".repeat(70) + "123", null)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new PerfilAcesso(null, "Perfil", null, true, java.util.Collections.singleton(null), 0))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(new PerfilAcesso(null, "Perfil", null, true, null, 0).permissoes()).isEmpty();
    }
}
