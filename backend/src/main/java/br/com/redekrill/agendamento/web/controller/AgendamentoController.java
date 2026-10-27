package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.application.dto.NovoAgendamento;
import br.com.redekrill.agendamento.application.dto.ResultadoAgendamento;
import br.com.redekrill.agendamento.application.dto.Simulacao;
import br.com.redekrill.agendamento.application.usecase.AgendamentoUseCase;
import br.com.redekrill.agendamento.application.usecase.AuditoriaUseCase;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import br.com.redekrill.agendamento.web.dto.Requisicoes;
import br.com.redekrill.agendamento.web.dto.Respostas.AgendamentoResp;
import br.com.redekrill.agendamento.web.dto.Respostas.CriacaoResp;
import br.com.redekrill.agendamento.web.dto.Respostas.OcupacaoDiaResp;
import br.com.redekrill.agendamento.web.dto.Respostas.SimulacaoResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Agendamentos")
public class AgendamentoController {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter BR_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AgendamentoUseCase useCase;
    private final AuditoriaUseCase auditoria;

    public AgendamentoController(AgendamentoUseCase useCase,
                                 AuditoriaUseCase auditoria) {
        this.useCase = useCase;
        this.auditoria = auditoria;
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria agendamento (não bloqueia por capacidade; devolve alertas)")
    public CriacaoResp criar(@Valid @RequestBody Requisicoes.CriarAgendamento b, HttpServletRequest req) {
        ResultadoAgendamento r = useCase.criar(new NovoAgendamento(b.data(), b.faixaHorariaId(), b.idPedido(),
                b.fornecedor(), b.emailFornecedor(), b.tipoCaminhaoId(), b.tipoPaletizacaoId(),
                b.quantidadePaletes(), b.observacao()), Requisicao.solicitante(req));
        return new CriacaoResp(AgendamentoResp.de(r.agendamento()), r.alertas(), r.linkFornecedor());
    }

    @GetMapping("/agendamentos")
    @Operation(summary = "Lista/consulta agendamentos por período e filtros")
    public List<AgendamentoResp> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) String fornecedor,
            @RequestParam(required = false) Long idPedido,
            @RequestParam(required = false) Long faixaHorariaId) {
        return useCase.listar(new FiltroAgendamento(inicio, fim, status, fornecedor, idPedido, faixaHorariaId))
                .stream().map(AgendamentoResp::de).toList();
    }

    @GetMapping("/agendamentos/{id}")
    public AgendamentoResp buscar(@PathVariable Long id) {
        return AgendamentoResp.de(useCase.buscar(id));
    }

    @PatchMapping("/agendamentos/{id}/cancelamento")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancela um agendamento (libera a capacidade)")
    public AgendamentoResp cancelar(@PathVariable Long id, @Valid @RequestBody Requisicoes.Cancelar b,
                                    HttpServletRequest req) {
        return AgendamentoResp.de(useCase.cancelar(id, b.motivo(), Requisicao.solicitante(req)));
    }

    @PostMapping("/agendamentos/simulacao")
    @Operation(summary = "Pré-visualiza ocupação e alertas antes de confirmar")
    public SimulacaoResp simular(@Valid @RequestBody Requisicoes.Simular b) {
        Simulacao s = useCase.simular(b.data(), b.faixaHorariaId(), b.tipoPaletizacaoId(),
                b.quantidadePaletes() == null ? 0 : b.quantidadePaletes());
        return new SimulacaoResp(OcupacaoDiaResp.de(s.ocupacaoAtual()), s.paletesEquivalentes(), s.alertas());
    }

    @GetMapping("/ocupacao")
    @Operation(summary = "Ocupação de um dia (total e por faixa)")
    public OcupacaoDiaResp ocupacao(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return OcupacaoDiaResp.de(useCase.ocupacaoDoDia(data));
    }

    @GetMapping("/ocupacao/periodo")
    @Operation(summary = "Ocupação diária em um período")
    public List<OcupacaoDiaResp> ocupacaoPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return useCase.ocupacaoPeriodo(inicio, fim).stream().map(OcupacaoDiaResp::de).toList();
    }

    @GetMapping(value = "/relatorios/agendamentos.csv", produces = "text/csv")
    @Operation(summary = "Exporta o relatório de agendamentos do período em CSV")
    public ResponseEntity<byte[]> exportar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusAgendamento status,
            HttpServletRequest req) {
        List<AgendamentoDetalhe> lista = useCase.listar(new FiltroAgendamento(inicio, fim, status, null, null, null));
        StringBuilder sb = new StringBuilder("\uFEFFID;Data;Faixa;ID Pedido;Fornecedor;E-mail;Caminhão;Paletização;"
                + "Paletes;Paletes equivalentes;Status;Criado por;Criado em;Observação\n");
        for (AgendamentoDetalhe d : lista) {
            var a = d.agendamento();
            sb.append(a.getId()).append(';').append(a.getData().format(BR)).append(';').append(d.faixaDescricao())
                    .append(';').append(a.getIdPedido()).append(';').append(csv(a.getFornecedor())).append(';')
                    .append(csv(a.getEmailFornecedor())).append(';').append(csv(d.tipoCaminhao())).append(';')
                    .append(csv(d.tipoPaletizacao())).append(';').append(a.getQuantidadePaletes()).append(';')
                    .append(a.getPaletesEquivalentes()).append(';').append(a.getStatus()).append(';')
                    .append(csv(a.getCriadoPor())).append(';')
                    .append(a.getCriadoEm() == null ? "" : a.getCriadoEm().atZoneSameInstant(java.time.ZoneId.of("America/Sao_Paulo")).format(BR_HORA))
                    .append(';').append(csv(a.getObservacao())).append('\n');
        }
        auditoria.registrar(Requisicao.solicitante(req), "EXPORTAR", "RELATORIO", null,
                Map.of("inicio", inicio.toString(), "fim", fim.toString(), "linhas", lista.size()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=agendamentos_" + inicio + "_" + fim + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Escapa campo CSV e neutraliza injeção de fórmula (=, +, -, @). */
    static String csv(String v) {
        if (v == null) return "";
        String s = v.replace("\r", " ").replace("\n", " ");
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(";") || s.contains("\"")) s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}
