package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.ValePaleteUseCase;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import br.com.redekrill.agendamento.web.dto.Respostas.ValePaleteResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/vales-palete")
@Tag(name = "Vale-palete")
public class ValePaleteController {

    private final ValePaleteUseCase useCase;

    public ValePaleteController(ValePaleteUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<ValePaleteResp> listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                       @RequestParam(required = false) ValePalete.Status status,
                                       @RequestParam(required = false) String fornecedor) {
        return useCase.listar(inicio, fim, status, fornecedor).stream().map(ValePaleteResp::de).toList();
    }

    @GetMapping("/{id}")
    public ValePaleteResp buscar(@PathVariable Long id) {
        return ValePaleteResp.de(useCase.buscar(id));
    }

    @GetMapping("/codigo/{codigo}")
    @Operation(summary = "Busca pelo código VP-... (leitura do código de barras/QR)")
    public ValePaleteResp porCodigo(@PathVariable String codigo) {
        return ValePaleteResp.de(useCase.buscarPorCodigo(codigo));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ValePaleteResp emitir(@Valid @RequestBody Requisicoes.NovoVale b, HttpServletRequest req) {
        return ValePaleteResp.de(useCase.emitir(new ValePaleteUseCase.NovoVale(b.agendamentoId(), b.data(), b.fornecedor(),
                b.placa(), b.motorista(), new ValePalete.Quantidades(b.quantidades()),
                b.observacao()), Requisicao.solicitante(req)));
    }

    @PutMapping("/{id}/assinaturas/{quem}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Grava assinatura do MOTORISTA ou CONFERENTE")
    public ValePaleteResp assinar(@PathVariable Long id, @PathVariable ValePalete.Assinante quem,
                                  @Valid @RequestBody Requisicoes.Assinatura b, HttpServletRequest req) {
        return ValePaleteResp.de(useCase.assinar(id, quem, b.nome(), b.imagem(), Requisicao.solicitante(req)));
    }

    @DeleteMapping("/{id}/assinaturas/{quem}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refazer assinatura")
    public ValePaleteResp refazer(@PathVariable Long id, @PathVariable ValePalete.Assinante quem, HttpServletRequest req) {
        return ValePaleteResp.de(useCase.refazerAssinatura(id, quem, Requisicao.solicitante(req)));
    }

    @PostMapping("/{id}/devolucoes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra devolução de paletes (total quita o vale)")
    public ValePaleteResp devolver(@PathVariable Long id, @Valid @RequestBody Requisicoes.Devolucao b, HttpServletRequest req) {
        return ValePaleteResp.de(useCase.registrarDevolucao(id,
                new ValePalete.Quantidades(b.quantidades()), Requisicao.solicitante(req)));
    }

    @PatchMapping("/{id}/cancelamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ValePaleteResp cancelar(@PathVariable Long id, HttpServletRequest req) {
        return ValePaleteResp.de(useCase.cancelar(id, Requisicao.solicitante(req)));
    }
}
