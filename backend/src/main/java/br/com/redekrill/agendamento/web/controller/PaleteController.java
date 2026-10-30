package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.PaleteFinanceiroUseCase;
import br.com.redekrill.agendamento.domain.model.CobrancaPalete;
import br.com.redekrill.agendamento.domain.model.MovimentoPalete;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/paletes")
@Tag(name = "Conta-corrente de paletes", description = "Saldo por fornecedor, cobrança, recibos e movimentos com as lojas")
public class PaleteController {

    public record CobrancaResp(Long id, String fornecedor, int quantidade, BigDecimal valorUnitario, BigDecimal valorTotal,
                               LocalDate vencimento, boolean vencida, String status, String observacao, String criadoPor,
                               OffsetDateTime criadoEm, String reciboNumero, String formaPagamento, OffsetDateTime pagoEm,
                               String recebidoPor, String motivoCancelamento) {
        static CobrancaResp de(CobrancaPalete c) {
            return new CobrancaResp(c.getId(), c.getFornecedor(), c.getQuantidade(), c.getValorUnitario(), c.valorTotal(),
                    c.getVencimento(), c.vencida(LocalDate.now()), c.getStatus().name(), c.getObservacao(), c.getCriadoPor(),
                    c.getCriadoEm(), c.getReciboNumero(), c.getFormaPagamento() == null ? null : c.getFormaPagamento().name(),
                    c.getPagoEm(), c.getRecebidoPor(), c.getMotivoCancelamento());
        }
    }

    public record MovimentoResp(Long id, LocalDate data, String tipo, Long cdId, String loja, String romaneio,
                                java.util.Map<String, Integer> quantidades, int total, String observacao, String criadoPor,
                                OffsetDateTime criadoEm) {
        static MovimentoResp de(MovimentoPalete m) {
            return new MovimentoResp(m.id(), m.data(), m.tipo().name(), m.cdId(), m.loja(), m.romaneio(), m.quantidades().itens(),
                    m.quantidades().total(), m.observacao(), m.criadoPor(), m.criadoEm());
        }
    }

    private final PaleteFinanceiroUseCase useCase;

    public PaleteController(PaleteFinanceiroUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/saldos")
    @Operation(summary = "Conta-corrente: saldo de paletes por fornecedor (vales − devoluções − cobranças)")
    public List<PaleteFinanceiroUseCase.SaldoFornecedor> saldos(@RequestParam(required = false) String fornecedor) {
        return useCase.saldoPorFornecedor(fornecedor);
    }

    @GetMapping("/cobrancas")
    public List<CobrancaResp> cobrancas(@RequestParam(required = false) String fornecedor,
                                        @RequestParam(required = false) CobrancaPalete.Status status) {
        return useCase.listarCobrancas(fornecedor, status).stream().map(CobrancaResp::de).toList();
    }

    @GetMapping("/cobrancas/{id}")
    @Operation(summary = "Cobrança / recibo (para impressão)")
    public CobrancaResp cobranca(@PathVariable Long id) {
        return CobrancaResp.de(useCase.cobranca(id));
    }

    @PostMapping("/cobrancas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gera cobrança de paletes não devolvidos (limitada ao saldo em aberto)")
    public CobrancaResp cobrar(@Valid @RequestBody Requisicoes.Cobranca b, HttpServletRequest req) {
        return CobrancaResp.de(useCase.cobrar(new PaleteFinanceiroUseCase.NovaCobranca(b.fornecedor(), b.quantidade(),
                b.valorUnitario(), b.vencimento(), b.observacao()), Requisicao.solicitante(req)));
    }

    @PostMapping("/cobrancas/{id}/pagamento")
    @Operation(summary = "Registra o pagamento e emite o recibo RP-AAAAMMDD-NNNN")
    public CobrancaResp pagar(@PathVariable Long id, @Valid @RequestBody Requisicoes.Pagamento b, HttpServletRequest req) {
        return CobrancaResp.de(useCase.pagar(id, b.forma(), Requisicao.solicitante(req)));
    }

    @PatchMapping("/cobrancas/{id}/cancelamento")
    public CobrancaResp cancelar(@PathVariable Long id, @Valid @RequestBody Requisicoes.Cancelar b, HttpServletRequest req) {
        return CobrancaResp.de(useCase.cancelarCobranca(id, b.motivo(), Requisicao.solicitante(req)));
    }

    @GetMapping("/movimentos")
    @Operation(summary = "Romaneios de envio/retorno de paletes com as lojas")
    public List<MovimentoResp> movimentos(@RequestParam(required = false) String loja,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return useCase.listarMovimentos(loja, inicio, fim).stream().map(MovimentoResp::de).toList();
    }

    @PostMapping("/movimentos")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentoResp registrar(@Valid @RequestBody Requisicoes.Movimento b, HttpServletRequest req) {
        return MovimentoResp.de(useCase.registrarMovimento(new PaleteFinanceiroUseCase.NovoMovimento(b.data(), b.tipo(),
                b.cdId(), b.loja(), new ValePalete.Quantidades(b.quantidades()),
                b.observacao()), Requisicao.solicitante(req)));
    }

    @GetMapping("/lojas")
    @Operation(summary = "Visão da loja: saldo de paletes de cada loja com o CD")
    public List<PaleteFinanceiroUseCase.SaldoLoja> lojas(@RequestParam(required = false) String loja) {
        return useCase.saldoPorLoja(loja);
    }
}
