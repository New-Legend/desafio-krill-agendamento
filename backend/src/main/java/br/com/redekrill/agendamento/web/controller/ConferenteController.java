package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.ConferenteUseCase;
import br.com.redekrill.agendamento.domain.model.Conferente;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/conferentes")
@Tag(name = "Conferentes", description = "Conferentes por CD (aparecem ao iniciar a conferência)")
public class ConferenteController {
    private final ConferenteUseCase useCase;

    public ConferenteController(ConferenteUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<Conferente> listar(@RequestParam(required = false) Long cdId,
                                   @RequestParam(defaultValue = "false") boolean ativos) {
        return useCase.listar(cdId, ativos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Conferente criar(@Valid @RequestBody Requisicoes.Conferente b, HttpServletRequest req) {
        return useCase.salvar(null, new ConferenteUseCase.Dados(b.cdId(), b.nome(), b.ativo()), Requisicao.solicitante(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Conferente atualizar(@PathVariable Long id, @Valid @RequestBody Requisicoes.Conferente b, HttpServletRequest req) {
        return useCase.salvar(id, new ConferenteUseCase.Dados(b.cdId(), b.nome(), b.ativo()), Requisicao.solicitante(req));
    }
}
