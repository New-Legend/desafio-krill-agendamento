package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.AgendamentoUseCase;
import br.com.redekrill.agendamento.application.usecase.DashboardUseCase;
import br.com.redekrill.agendamento.application.usecase.PedidoCompraUseCase;
import br.com.redekrill.agendamento.application.usecase.RecebimentoUseCase;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import br.com.redekrill.agendamento.web.dto.Respostas.AgendamentoResp;
import br.com.redekrill.agendamento.web.dto.Respostas.OcupacaoDiaResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Recebimento operacional", description = "Portaria, conferência, dashboard e integração ERP")
public class RecebimentoController {

    private final RecebimentoUseCase recebimento;
    private final AgendamentoUseCase agendamentos;
    private final DashboardUseCase dashboard;
    private final PedidoCompraUseCase pedidos;

    public RecebimentoController(RecebimentoUseCase recebimento, AgendamentoUseCase agendamentos, DashboardUseCase dashboard,
                                 PedidoCompraUseCase pedidos) {
        this.recebimento = recebimento;
        this.agendamentos = agendamentos;
        this.dashboard = dashboard;
        this.pedidos = pedidos;
    }

    @PatchMapping("/agendamentos/{id}/status")
    @Operation(summary = "Avança o fluxo: CONFIRMAR, CHEGADA (portaria), INICIAR (conferência), CONCLUIR ou NAO_COMPARECEU")
    public AgendamentoResp acao(@PathVariable Long id, @Valid @RequestBody Requisicoes.AcaoRecebimento b, HttpServletRequest req) {
        return AgendamentoResp.de(recebimento.executar(id, new RecebimentoUseCase.Comando(b.acao(), b.placa(), b.motorista(),
                b.conferente(), b.paletesRecebidos(), b.divergencia(), b.motivo()), Requisicao.solicitante(req)));
    }

    @PostMapping("/agendamentos/{id}/link-fornecedor")
    @Operation(summary = "Gera novo link público para o fornecedor (invalida o anterior)")
    public Map<String, String> novoLink(@PathVariable Long id, HttpServletRequest req) {
        return Map.of("linkFornecedor", agendamentos.gerarNovoLinkFornecedor(id, Requisicao.solicitante(req)));
    }

    public record DashboardResp(LocalDate data, OcupacaoDiaResp ocupacao, Map<String, Long> porStatus,
                                List<AgendamentoResp> proximos, List<DashboardUseCase.Alerta> alertas,
                                List<OcupacaoDiaResp> proximosDias) {
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Home: ocupação do dia, por faixa, próximos caminhões, status e alertas")
    public DashboardResp dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        DashboardUseCase.Resumo r = dashboard.resumo(data);
        Map<String, Long> status = new java.util.LinkedHashMap<>();
        r.porStatus().forEach((k, v) -> status.put(k.name(), v));
        return new DashboardResp(r.data(), OcupacaoDiaResp.de(r.ocupacao()), status,
                r.proximos().stream().map(AgendamentoResp::de).toList(), r.alertas(),
                r.proximosDias().stream().map(OcupacaoDiaResp::de).toList());
    }

    @GetMapping("/erp/pedidos/{idPedido}")
    @Operation(summary = "Consulta pedido de compra no ERP (Bluesoft ou adaptador local) para preencher o agendamento")
    public PedidoCompraUseCase.Sugestao pedido(@PathVariable Long idPedido) {
        return pedidos.consultar(idPedido);
    }
}
