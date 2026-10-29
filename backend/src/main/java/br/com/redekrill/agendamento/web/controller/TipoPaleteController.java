package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.TipoPaleteUseCase;
import br.com.redekrill.agendamento.domain.model.TipoPalete;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-palete")
@Tag(name = "Tipos de palete", description = "Configuração do vale-palete: tipos aceitos (PBR, CHEP, Chapatex, descartável...)")
public class TipoPaleteController {
    private final TipoPaleteUseCase useCase;

    public TipoPaleteController(TipoPaleteUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<TipoPalete> listar(@RequestParam(defaultValue = "false") boolean ativos) {
        return useCase.listar(ativos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TipoPalete criar(@Valid @RequestBody Requisicoes.TipoPalete b, HttpServletRequest req) {
        return useCase.criar(dados(b), Requisicao.solicitante(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Altera nome, valor de reposição, ordem e situação (o código não muda)")
    public TipoPalete atualizar(@PathVariable Long id, @Valid @RequestBody Requisicoes.TipoPalete b, HttpServletRequest req) {
        return useCase.atualizar(id, dados(b), Requisicao.solicitante(req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exclui tipo nunca usado (usado deve ser inativado)")
    public void excluir(@PathVariable Long id, HttpServletRequest req) {
        useCase.excluir(id, Requisicao.solicitante(req));
    }

    private static TipoPaleteUseCase.Dados dados(Requisicoes.TipoPalete b) {
        return new TipoPaleteUseCase.Dados(b.codigo(), b.nome(), b.valorReposicao(), b.ordem(), b.ativo());
    }
}
