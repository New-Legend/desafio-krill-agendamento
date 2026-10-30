package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.TaxaDescargaUseCase;
import br.com.redekrill.agendamento.domain.model.CalculadoraTaxaDescarga;
import br.com.redekrill.agendamento.domain.model.TaxaDescarga;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/taxas-descarga")
@Tag(name = "Taxa de descarga", description = "Taxa por palete conforme o modal (sider, baú, granel...)")
public class TaxaDescargaController {
    private final TaxaDescargaUseCase useCase;

    public TaxaDescargaController(TaxaDescargaUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<TaxaDescarga> listar() {
        return useCase.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxaDescarga criar(@Valid @RequestBody Requisicoes.TaxaDescarga b, HttpServletRequest req) {
        return useCase.salvar(null, dados(b), Requisicao.solicitante(req));
    }

    @PutMapping("/{id}")
    public TaxaDescarga alterar(@PathVariable Long id, @Valid @RequestBody Requisicoes.TaxaDescarga b, HttpServletRequest req) {
        return useCase.salvar(id, dados(b), Requisicao.solicitante(req));
    }

    @GetMapping("/calculo")
    @Operation(summary = "Calcula a taxa de descarga (modal informado ou deduzido do tipo de caminhão)")
    public CalculadoraTaxaDescarga.Cobranca calcular(@RequestParam(required = false) String modal,
                                                     @RequestParam(required = false) String tipoCaminhao,
                                                     @RequestParam int paletes) {
        return useCase.calcular(modal, tipoCaminhao, paletes)
                .orElseThrow(() -> new br.com.redekrill.agendamento.domain.exception.RegraNegocioException("Modal sem taxa cadastrada"));
    }

    private static TaxaDescargaUseCase.Dados dados(Requisicoes.TaxaDescarga b) {
        return new TaxaDescargaUseCase.Dados(b.modal(), b.nome(), b.valorPorPalete(), b.ativo());
    }
}
