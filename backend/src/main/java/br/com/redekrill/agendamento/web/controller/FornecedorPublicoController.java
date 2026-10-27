package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.FornecedorLinkUseCase;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import br.com.redekrill.agendamento.web.dto.Respostas.AgendamentoPublicoResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos acessados pelo link secreto enviado no e-mail do fornecedor. */
@RestController
@RequestMapping("/api/v1/publico/fornecedor/{token}")
@Tag(name = "Link público do fornecedor")
@SecurityRequirements
public class FornecedorPublicoController {

    private final FornecedorLinkUseCase useCase;

    public FornecedorPublicoController(FornecedorLinkUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @Operation(summary = "Dados do agendamento para o fornecedor")
    public AgendamentoPublicoResp consultar(@PathVariable String token) {
        return AgendamentoPublicoResp.de(useCase.consultar(token));
    }

    @PostMapping("/confirmacao")
    @Operation(summary = "Fornecedor confirma presença")
    public AgendamentoPublicoResp confirmar(@PathVariable String token, HttpServletRequest req) {
        return AgendamentoPublicoResp.de(useCase.confirmar(token, Requisicao.ip(req)));
    }

    @PutMapping("/veiculo")
    @Operation(summary = "Fornecedor informa placa e motorista")
    public AgendamentoPublicoResp veiculo(@PathVariable String token, @Valid @RequestBody Requisicoes.VeiculoFornecedor b,
                                          HttpServletRequest req) {
        return AgendamentoPublicoResp.de(useCase.informarVeiculo(token, b.placa(), b.motorista(), Requisicao.ip(req)));
    }
}
