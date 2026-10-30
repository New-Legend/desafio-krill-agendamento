package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.AcessoUseCase;
import br.com.redekrill.agendamento.domain.model.Permissao;
import br.com.redekrill.agendamento.infrastructure.security.AutenticacaoService;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AutenticacaoService autenticacao;
    private final AcessoUseCase acessos;

    public AuthController(AutenticacaoService autenticacao, AcessoUseCase acessos) {
        this.autenticacao = autenticacao;
        this.acessos = acessos;
    }

    public record TrocaSenha(@NotBlank String senhaAtual, @NotBlank String novaSenha) {
    }

    public record Eu(String usuario, String nome, String perfil, List<Integer> permissoes, boolean trocarSenha) {
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve o token JWT com as permissões do perfil")
    public AutenticacaoService.Sessao login(@Valid @RequestBody Requisicoes.Login body, HttpServletRequest req) {
        return autenticacao.autenticar(body.usuario(), body.senha(), Requisicao.origem(req));
    }

    @PostMapping("/logout")
    @Operation(summary = "Registra a saída no log de acessos")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        acessos.sair(Requisicao.solicitante(req).usuario(), Requisicao.origem(req));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/eu")
    @Operation(summary = "Usuário logado, perfil e permissões atuais")
    public Eu eu(HttpServletRequest req) {
        var ctx = acessos.contextoAtual(Requisicao.solicitante(req).usuario()).orElseThrow();
        return new Eu(ctx.usuario().login(), ctx.usuario().nome(), ctx.perfil().nome(),
                ctx.permissoes().stream().sorted().toList(), ctx.usuario().trocarSenha());
    }

    @PostMapping("/senha")
    @Operation(summary = "Troca a própria senha (obrigatória no primeiro acesso ou após redefinição)")
    public ResponseEntity<Void> trocarSenha(@Valid @RequestBody TrocaSenha body, HttpServletRequest req) {
        acessos.trocarSenha(Requisicao.solicitante(req).usuario(), body.senhaAtual(), body.novaSenha(), Requisicao.origem(req));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissoes")
    @Operation(summary = "Catálogo de permissões numeradas, com as telas onde cada uma aparece")
    public List<Map<String, Object>> catalogo() {
        return java.util.Arrays.stream(Permissao.values()).map(p -> Map.<String, Object>of("codigo", p.codigo(),
                "modulo", p.modulo(), "nome", p.nome(), "telas", p.telas())).toList();
    }
}
