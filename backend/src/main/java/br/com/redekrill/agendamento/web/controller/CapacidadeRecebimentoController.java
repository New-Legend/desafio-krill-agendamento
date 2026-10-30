package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.CapacidadeRecebimentoUseCase;
import br.com.redekrill.agendamento.domain.model.CapacidadeRecebimento;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import br.com.redekrill.agendamento.web.dto.Respostas.OcupacaoRecebimentoResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/capacidades-recebimento")
@Tag(name = "Capacidade de recebimento", description = "Capacidade diária por CD e por loja (paletes e/ou caminhões, por dia da semana)")
public class CapacidadeRecebimentoController {
    private final CapacidadeRecebimentoUseCase useCase;

    public CapacidadeRecebimentoController(CapacidadeRecebimentoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<CapacidadeRecebimento> listar() {
        return useCase.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CapacidadeRecebimento criar(@Valid @RequestBody Requisicoes.CapacidadeRecebimento b, HttpServletRequest req) {
        return useCase.salvar(null, dados(b), Requisicao.solicitante(req));
    }

    @PutMapping("/{id}")
    public CapacidadeRecebimento alterar(@PathVariable Long id, @Valid @RequestBody Requisicoes.CapacidadeRecebimento b, HttpServletRequest req) {
        return useCase.salvar(id, dados(b), Requisicao.solicitante(req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, HttpServletRequest req) {
        useCase.excluir(id, Requisicao.solicitante(req));
    }

    @GetMapping("/ocupacao")
    @Operation(summary = "Ocupação do dia do CD (ou da loja, para entrega direta) e se já está cheio")
    public OcupacaoRecebimentoResp ocupacao(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                            @RequestParam(required = false) Long cdId, @RequestParam(required = false) String loja) {
        return OcupacaoRecebimentoResp.de(useCase.ocupacao(data, cdId, loja));
    }

    private static CapacidadeRecebimentoUseCase.Dados dados(Requisicoes.CapacidadeRecebimento b) {
        return new CapacidadeRecebimentoUseCase.Dados(b.cdId(), b.loja(), b.diaSemana(), b.paletes(), b.caminhoes());
    }
}
