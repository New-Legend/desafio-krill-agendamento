package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.infrastructure.security.AutenticacaoService;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AutenticacaoService autenticacao;

    public AuthController(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve o token JWT")
    public AutenticacaoService.Sessao login(@Valid @RequestBody Requisicoes.Login body, HttpServletRequest req) {
        return autenticacao.autenticar(body.usuario(), body.senha(), Requisicao.ip(req));
    }
}
