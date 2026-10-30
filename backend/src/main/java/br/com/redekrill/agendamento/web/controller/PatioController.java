package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.PatioUseCase;
import br.com.redekrill.agendamento.domain.model.Doca;
import br.com.redekrill.agendamento.domain.model.LogisticaAgendamento;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1")
@Tag(name = "Pátio e docas", description = "Docas por CD, quadro do pátio, atribuição de doca e lotação do CD")
public class PatioController {
    private final PatioUseCase useCase;

    public PatioController(PatioUseCase useCase) {
        this.useCase = useCase;
    }

    public record LogisticaResp(LogisticaAgendamento logistica, List<String> alertas) {
    }

    @GetMapping("/docas")
    public List<Doca> docas(@RequestParam(required = false) Long cdId) {
        return useCase.listarDocas(cdId);
    }

    @PostMapping("/docas")
    @ResponseStatus(HttpStatus.CREATED)
    public Doca criarDoca(@Valid @RequestBody Requisicoes.Doca b, HttpServletRequest req) {
        return useCase.salvarDoca(null, dados(b), Requisicao.solicitante(req));
    }

    @PutMapping("/docas/{id}")
    public Doca alterarDoca(@PathVariable Long id, @Valid @RequestBody Requisicoes.Doca b, HttpServletRequest req) {
        return useCase.salvarDoca(id, dados(b), Requisicao.solicitante(req));
    }

    @GetMapping("/patio")
    @Operation(summary = "Quadro do pátio do dia: previstos, aguardando doca, na doca, descarregando e liberados, com a lotação do CD")
    public PatioUseCase.Quadro quadro(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                      @RequestParam(required = false) Long cdId) {
        return useCase.quadro(data, cdId);
    }

    @PutMapping("/agendamentos/{id}/doca")
    @Operation(summary = "Atribui a doca ao caminhão que chegou (docaId nulo libera a doca)")
    public LogisticaAgendamento atribuirDoca(@PathVariable Long id, @RequestBody Requisicoes.AtribuirDoca b, HttpServletRequest req) {
        return useCase.atribuirDoca(id, b.docaId(), Requisicao.solicitante(req));
    }

    @GetMapping("/agendamentos/{id}/logistica")
    public LogisticaAgendamento logistica(@PathVariable Long id) {
        return useCase.logistica(id);
    }

    @PutMapping("/agendamentos/{id}/logistica")
    @Operation(summary = "Loja (entrega direta), transportadora e modal de descarga; devolve alertas de capacidade do destino")
    public LogisticaResp alterarLogistica(@PathVariable Long id, @Valid @RequestBody Requisicoes.Logistica b, HttpServletRequest req) {
        List<String> alertas = useCase.registrarLogistica(id, b.loja(), b.transportadora(), b.modalDescarga(), Requisicao.solicitante(req));
        return new LogisticaResp(useCase.logistica(id), alertas);
    }

    private static PatioUseCase.DadosDoca dados(Requisicoes.Doca b) {
        return new PatioUseCase.DadosDoca(b.cdId(), b.codigo(), b.nome(), b.ativa());
    }
}
