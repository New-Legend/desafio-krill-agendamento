package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.AcessoUseCase;
import br.com.redekrill.agendamento.domain.model.EventoAcesso;
import br.com.redekrill.agendamento.domain.model.LogAcesso;
import br.com.redekrill.agendamento.domain.model.PerfilAcesso;
import br.com.redekrill.agendamento.domain.model.UsuarioSistema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Usuários, perfis e logs de acesso")
public class AcessoController {

    private final AcessoUseCase acessos;

    public AcessoController(AcessoUseCase acessos) {
        this.acessos = acessos;
    }

    public record Usuario(Long id, String login, String nome, Long perfilId, String perfil, boolean ativo,
                          boolean trocarSenha, Instant ultimoAcesso, Instant criadoEm) {
        static Usuario de(UsuarioSistema u) {
            return new Usuario(u.id(), u.login(), u.nome(), u.perfilId(), u.perfilNome(), u.ativo(), u.trocarSenha(),
                    u.ultimoAcesso(), u.criadoEm());
        }
    }

    public record DadosUsuario(@Size(max = 60) String login, @NotBlank @Size(max = 120) String nome, Long perfilId,
                               Boolean ativo, @Size(max = 72) String senhaProvisoria) {
    }

    public record NovaSenha(@NotBlank @Size(max = 72) String senhaProvisoria) {
    }

    public record DadosPerfil(@NotBlank @Size(max = 40) String nome, @Size(max = 160) String descricao, Boolean ativo,
                              Set<Integer> permissoes) {
    }

    public record Log(Long id, String usuario, EventoAcesso evento, String rotulo, String ip, String navegador,
                      String userAgent, Instant criadoEm) {
        static Log de(LogAcesso l) {
            return new Log(l.id(), l.usuario(), l.evento(), l.evento().rotulo(), l.ip(), LogAcesso.resumoNavegador(l.navegador()),
                    l.navegador(), l.criadoEm());
        }
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Lista usuários (permissão 1001)")
    public List<Usuario> usuarios(@RequestParam(required = false) String texto, @RequestParam(required = false) Long perfilId,
                                  @RequestParam(required = false) Boolean ativo) {
        return acessos.listarUsuarios(texto, perfilId, ativo).stream().map(Usuario::de).toList();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inclui usuário com senha provisória (permissão 1002)")
    public Usuario criarUsuario(@Valid @RequestBody DadosUsuario d, HttpServletRequest req) {
        return Usuario.de(acessos.salvarUsuario(null, dados(d), Requisicao.solicitante(req)));
    }

    @PutMapping("/usuarios/{id}")
    @Operation(summary = "Altera nome, perfil e situação (permissão 1002)")
    public Usuario alterarUsuario(@PathVariable Long id, @Valid @RequestBody DadosUsuario d, HttpServletRequest req) {
        return Usuario.de(acessos.salvarUsuario(id, dados(d), Requisicao.solicitante(req)));
    }

    @PostMapping("/usuarios/{id}/senha")
    @Operation(summary = "Redefine a senha; o usuário troca no próximo acesso (permissão 1002)")
    public ResponseEntity<Void> redefinirSenha(@PathVariable Long id, @Valid @RequestBody NovaSenha d, HttpServletRequest req) {
        acessos.redefinirSenha(id, d.senhaProvisoria(), Requisicao.solicitante(req));
        return ResponseEntity.noContent().build();
    }

    private static AcessoUseCase.DadosUsuario dados(DadosUsuario d) {
        return new AcessoUseCase.DadosUsuario(d.login(), d.nome(), d.perfilId(), d.ativo(), d.senhaProvisoria());
    }

    @GetMapping("/perfis")
    @Operation(summary = "Perfis com as permissões numeradas")
    public List<PerfilAcesso> perfis() {
        return acessos.listarPerfis();
    }

    @PostMapping("/perfis")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inclui perfil (permissão 1003)")
    public PerfilAcesso criarPerfil(@Valid @RequestBody DadosPerfil d, HttpServletRequest req) {
        return acessos.salvarPerfil(null, perfil(d), Requisicao.solicitante(req));
    }

    @PutMapping("/perfis/{id}")
    @Operation(summary = "Altera perfil e suas permissões (permissão 1003)")
    public PerfilAcesso alterarPerfil(@PathVariable Long id, @Valid @RequestBody DadosPerfil d, HttpServletRequest req) {
        return acessos.salvarPerfil(id, perfil(d), Requisicao.solicitante(req));
    }

    private static AcessoUseCase.DadosPerfil perfil(DadosPerfil d) {
        return new AcessoUseCase.DadosPerfil(d.nome(), d.descricao(), d.ativo(), d.permissoes());
    }

    @GetMapping("/logs-acesso")
    @Operation(summary = "Entradas, saídas e senhas recusadas (permissão 1004)")
    public List<Log> logs(@RequestParam(required = false) String texto, @RequestParam(required = false) EventoAcesso evento,
                          @RequestParam(defaultValue = "7") int dias, @RequestParam(defaultValue = "200") int limite) {
        return acessos.logs(texto, evento, dias, limite).stream().map(Log::de).toList();
    }
}
