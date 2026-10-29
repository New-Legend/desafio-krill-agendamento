package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.port.LogisticaRepositorio;
import br.com.redekrill.agendamento.application.usecase.ConfiguracaoValePaleteUseCase;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Configurações do vale-palete", description = "Exigir as duas assinaturas e agrupamento por fornecedor ou transportadora")
public class ValePaleteConfiguracaoController {
    private final ConfiguracaoValePaleteUseCase useCase;
    private final LogisticaRepositorio logistica;

    public ValePaleteConfiguracaoController(ConfiguracaoValePaleteUseCase useCase, LogisticaRepositorio logistica) {
        this.useCase = useCase;
        this.logistica = logistica;
    }

    @GetMapping("/vales-palete/configuracao")
    public ConfiguracaoValePaleteUseCase.Config obter() {
        return useCase.obter();
    }

    @PutMapping("/vales-palete/configuracao")
    @PreAuthorize("hasRole('ADMIN')")
    public ConfiguracaoValePaleteUseCase.Config salvar(@Valid @RequestBody Requisicoes.ConfiguracaoValePalete b, HttpServletRequest req) {
        return useCase.salvar(new ConfiguracaoValePaleteUseCase.Config(Boolean.TRUE.equals(b.exigirDuasAssinaturas()), b.agrupamento()),
                Requisicao.solicitante(req));
    }

    @GetMapping("/paletes/saldos-transportadora")
    @Operation(summary = "Conta-corrente de vales agrupada pela transportadora do agendamento (ou fornecedor, quando não informada)")
    public List<LogisticaRepositorio.SaldoTransportadora> saldosPorTransportadora() {
        return logistica.saldosPorTransportadora();
    }
}
