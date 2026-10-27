package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.application.usecase.RelatorioRecebimentoUseCase;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.web.dto.Respostas.AgendamentoResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static br.com.redekrill.agendamento.web.controller.AgendamentoController.csv;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Relatórios do recebimento", description = "Divergências, tempos de descarga e rotina diária")
public class RelatorioRecebimentoController {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record DivergenciaResp(AgendamentoResp agendamento, int diferencaPaletes, int notas, boolean pesoNfDivergente) {
    }

    public record LinhaDescargaResp(AgendamentoResp agendamento, br.com.redekrill.agendamento.domain.service.CalculadoraDescarga.Indicador indicador) {
    }

    public record DescargaResp(List<LinhaDescargaResp> linhas, List<RelatorioRecebimentoUseCase.ResumoDescarga> resumo) {
    }

    private final RelatorioRecebimentoUseCase useCase;

    public RelatorioRecebimentoController(RelatorioRecebimentoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/divergencias")
    @Operation(summary = "Recebimentos com divergência no período")
    public List<DivergenciaResp> divergencias(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                              @RequestParam(required = false) String fornecedor,
                                              @RequestParam(required = false) Long cdId) {
        return useCase.divergencias(inicio, fim, fornecedor, cdId).stream()
                .map(d -> new DivergenciaResp(AgendamentoResp.de(d.agendamento()), d.diferencaPaletes(), d.notas(), d.pesoNfDivergente()))
                .toList();
    }

    @GetMapping(value = "/divergencias.csv", produces = "text/csv")
    @Operation(summary = "Exporta as divergências do período em CSV")
    public ResponseEntity<byte[]> divergenciasCsv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                                  @RequestParam(required = false) String fornecedor,
                                                  @RequestParam(required = false) Long cdId) {
        StringBuilder sb = new StringBuilder("\uFEFFID;Data;Faixa;Pedido;Fornecedor;Placa;Conferente;Paletes agendados;"
                + "Paletes recebidos;Diferença;NF-e;Peso NF divergente;Divergência\n");
        for (var d : useCase.divergencias(inicio, fim, fornecedor, cdId)) {
            Agendamento a = d.agendamento().agendamento();
            sb.append(a.getId()).append(';').append(a.getData().format(BR)).append(';').append(d.agendamento().faixaDescricao())
                    .append(';').append(a.getIdPedido()).append(';').append(csv(a.getFornecedor())).append(';')
                    .append(csv(a.getPlaca())).append(';').append(csv(a.getConferente())).append(';')
                    .append(a.getQuantidadePaletes()).append(';').append(a.getPaletesRecebidos() == null ? "" : a.getPaletesRecebidos())
                    .append(';').append(d.diferencaPaletes()).append(';').append(d.notas()).append(';')
                    .append(d.pesoNfDivergente() ? "Sim" : "Não").append(';').append(csv(a.getDivergencia())).append('\n');
        }
        return arquivo(sb, "divergencias_" + inicio + "_" + fim + ".csv");
    }

    @GetMapping("/relatorios/descarga")
    @Operation(summary = "Tempos de espera e descarga, previsto x realizado e custo")
    public DescargaResp descarga(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                 @RequestParam(required = false) Long cdId) {
        var r = useCase.descarga(inicio, fim, cdId);
        return new DescargaResp(r.linhas().stream().map(l -> new LinhaDescargaResp(AgendamentoResp.de(l.agendamento()),
                l.indicador())).toList(), r.resumo());
    }

    @GetMapping(value = "/relatorios/descarga.csv", produces = "text/csv")
    public ResponseEntity<byte[]> descargaCsv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                              @RequestParam(required = false) Long cdId) {
        StringBuilder sb = new StringBuilder("\uFEFFID;Data;Fornecedor;Caminhão;Placa;Espera (min);Descarga (min);"
                + "Previsto (min);Excesso (min);Situação;Custo (R$)\n");
        for (var l : useCase.descarga(inicio, fim, cdId).linhas()) {
            Agendamento a = l.agendamento().agendamento();
            var i = l.indicador();
            sb.append(a.getId()).append(';').append(a.getData().format(BR)).append(';').append(csv(a.getFornecedor())).append(';')
                    .append(csv(l.agendamento().tipoCaminhao())).append(';').append(csv(a.getPlaca())).append(';')
                    .append(n(i.esperaMinutos())).append(';').append(n(i.descargaMinutos())).append(';').append(n(i.previstoMinutos()))
                    .append(';').append(n(i.excessoMinutos())).append(';').append(i.situacao()).append(';')
                    .append(i.custo() == null ? "" : i.custo().toPlainString().replace('.', ',')).append('\n');
        }
        return arquivo(sb, "descarga_" + inicio + "_" + fim + ".csv");
    }

    @GetMapping("/rotina")
    @Operation(summary = "Rotina diária: pendências que precisam de ação")
    public RelatorioRecebimentoUseCase.Rotina rotina(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                                     @RequestParam(required = false) Long cdId) {
        return useCase.rotina(data, cdId);
    }

    private static String n(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static ResponseEntity<byte[]> arquivo(StringBuilder sb, String nome) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nome)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
