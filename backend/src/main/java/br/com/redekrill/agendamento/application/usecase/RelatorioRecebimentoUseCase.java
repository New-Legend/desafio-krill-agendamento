package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.application.port.CobrancaPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.FaixaHorariaRepositorio;
import br.com.redekrill.agendamento.application.port.NotaFiscalRepositorio;
import br.com.redekrill.agendamento.application.port.ParametroDescargaRepositorio;
import br.com.redekrill.agendamento.application.port.ValePaleteRepositorio;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.CobrancaPalete;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.NotaFiscal;
import br.com.redekrill.agendamento.domain.model.ParametroDescarga;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import br.com.redekrill.agendamento.domain.service.CalculadoraDescarga;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Relatórios operacionais: divergências, tempos de descarga e rotina diária do recebimento. */
public class RelatorioRecebimentoUseCase {

    static final int ATRASO_MINUTOS = 30;

    public record Divergencia(AgendamentoDetalhe agendamento, int diferencaPaletes, int notas, boolean pesoNfDivergente) {
    }

    public record LinhaDescarga(AgendamentoDetalhe agendamento, CalculadoraDescarga.Indicador indicador) {
    }

    public record ResumoDescarga(String tipoCaminhao, int recebimentos, double mediaEsperaMinutos,
                                 double mediaDescargaMinutos, Integer previstoMinutos, int acimaDoPrevisto,
                                 BigDecimal custoTotal) {
    }

    public record Descarga(List<LinhaDescarga> linhas, List<ResumoDescarga> resumo) {
    }

    public record Referencia(Long id, String descricao, String link) {
    }

    public record ItemRotina(String codigo, String titulo, String nivel, List<Referencia> itens) {
    }

    public record Rotina(LocalDate data, List<ItemRotina> itens) {
    }

    private final AgendamentoUseCase agendamentos;
    private final FaixaHorariaRepositorio faixas;
    private final ParametroDescargaRepositorio parametros;
    private final NotaFiscalRepositorio notas;
    private final ValePaleteRepositorio vales;
    private final CobrancaPaleteRepositorio cobrancas;
    private final Clock clock;
    private final CalculadoraDescarga calculadora = new CalculadoraDescarga();

    public RelatorioRecebimentoUseCase(AgendamentoUseCase agendamentos, FaixaHorariaRepositorio faixas,
                                       ParametroDescargaRepositorio parametros, NotaFiscalRepositorio notas,
                                       ValePaleteRepositorio vales, CobrancaPaleteRepositorio cobrancas, Clock clock) {
        this.agendamentos = agendamentos;
        this.faixas = faixas;
        this.parametros = parametros;
        this.notas = notas;
        this.vales = vales;
        this.cobrancas = cobrancas;
        this.clock = clock;
    }

    public List<Divergencia> divergencias(LocalDate inicio, LocalDate fim, String fornecedor, Long cdId) {
        exigirPeriodo(inicio, fim);
        List<AgendamentoDetalhe> lista = agendamentos.listar(
                new FiltroAgendamento(inicio, fim, null, fornecedor, null, null, cdId, true));
        Map<Long, List<NotaFiscal>> nf = notasPor(lista);
        return lista.stream().map(d -> {
            Agendamento a = d.agendamento();
            List<NotaFiscal> n = nf.getOrDefault(a.getId(), List.of());
            int recebidos = a.getPaletesRecebidos() == null ? a.getQuantidadePaletes() : a.getPaletesRecebidos();
            return new Divergencia(d, recebidos - a.getQuantidadePaletes(), n.size(),
                    n.stream().anyMatch(NotaFiscal::pesoDivergente));
        }).toList();
    }

    public Descarga descarga(LocalDate inicio, LocalDate fim, Long cdId) {
        exigirPeriodo(inicio, fim);
        Map<Long, ParametroDescarga> p = parametros.listar().stream()
                .collect(Collectors.toMap(ParametroDescarga::tipoCaminhaoId, Function.identity()));
        OffsetDateTime agora = OffsetDateTime.now(clock);
        List<LinhaDescarga> linhas = agendamentos.listar(new FiltroAgendamento(inicio, fim, null, null, null, null, cdId, false))
                .stream().filter(d -> d.agendamento().getChegadaEm() != null)
                .map(d -> new LinhaDescarga(d, calculadora.calcular(d.agendamento(), p.get(d.agendamento().getTipoCaminhaoId()), agora)))
                .toList();
        Map<String, List<LinhaDescarga>> porTipo = linhas.stream()
                .collect(Collectors.groupingBy(l -> l.agendamento().tipoCaminhao(), LinkedHashMap::new, Collectors.toList()));
        List<ResumoDescarga> resumo = porTipo.entrySet().stream().map(e -> {
            List<CalculadoraDescarga.Indicador> ind = e.getValue().stream().map(LinhaDescarga::indicador).toList();
            return new ResumoDescarga(e.getKey(), ind.size(),
                    media(ind.stream().map(CalculadoraDescarga.Indicador::esperaMinutos).toList()),
                    media(ind.stream().map(CalculadoraDescarga.Indicador::descargaMinutos).toList()),
                    ind.get(0).previstoMinutos(),
                    (int) ind.stream().filter(i -> i.situacao() == CalculadoraDescarga.Situacao.ACIMA_DO_PREVISTO).count(),
                    ind.stream().map(CalculadoraDescarga.Indicador::custo).filter(java.util.Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
        }).sorted(Comparator.comparing(ResumoDescarga::tipoCaminhao)).toList();
        return new Descarga(linhas, resumo);
    }

    /** Checklist do dia (porte de /recebimento/rotina): o que precisa de ação agora. */
    public Rotina rotina(LocalDate data, Long cdId) {
        LocalDate dia = data == null ? LocalDate.now(clock) : data;
        LocalDate hoje = LocalDate.now(clock);
        OffsetDateTime agora = OffsetDateTime.now(clock);
        Map<Long, FaixaHoraria> f = faixas.listar().stream().collect(Collectors.toMap(FaixaHoraria::id, Function.identity()));
        Map<Long, ParametroDescarga> p = parametros.listar().stream()
                .collect(Collectors.toMap(ParametroDescarga::tipoCaminhaoId, Function.identity()));
        List<AgendamentoDetalhe> doDia = agendamentos.listar(new FiltroAgendamento(dia, dia, null, null, null, null, cdId, false));
        List<AgendamentoDetalhe> amanha = agendamentos.listar(
                new FiltroAgendamento(dia.plusDays(1), dia.plusDays(1), StatusAgendamento.AGENDADO, null, null, null, cdId, false));
        List<ItemRotina> itens = new ArrayList<>();

        itens.add(item("SEM_CONFIRMACAO", "Entregas de amanhã sem confirmação do fornecedor", "aviso", amanha));
        LocalTime limite = LocalTime.now(clock).minusMinutes(ATRASO_MINUTOS);
        itens.add(item("ATRASADOS", "Caminhões atrasados (faixa começou há mais de " + ATRASO_MINUTOS + " min)", "erro",
                doDia.stream().filter(d -> dia.equals(hoje) && d.agendamento().getStatus().antesDaChegada())
                        .filter(d -> {
                            FaixaHoraria fx = f.get(d.agendamento().getFaixaHorariaId());
                            return fx != null && fx.horaInicio().isBefore(limite);
                        }).toList()));
        itens.add(item("ESPERA", "Na portaria acima da tolerância de espera", "erro",
                doDia.stream().filter(d -> d.agendamento().getStatus() == StatusAgendamento.CHEGOU)
                        .filter(d -> calculadora.calcular(d.agendamento(), p.get(d.agendamento().getTipoCaminhaoId()), agora)
                                .esperaAcimaTolerancia()).toList()));
        itens.add(item("DESCARGA_LONGA", "Descarga acima do tempo previsto", "aviso",
                doDia.stream().filter(d -> d.agendamento().getStatus() == StatusAgendamento.EM_RECEBIMENTO)
                        .filter(d -> {
                            CalculadoraDescarga.Indicador i = calculadora.calcular(d.agendamento(),
                                    p.get(d.agendamento().getTipoCaminhaoId()), agora);
                            return i.excessoMinutos() != null && i.excessoMinutos() > 0;
                        }).toList()));
        Map<Long, List<NotaFiscal>> nf = notasPor(doDia);
        itens.add(item("SEM_NFE", "Recebidos sem XML da NF-e importado", "aviso",
                doDia.stream().filter(d -> d.agendamento().getStatus() == StatusAgendamento.RECEBIDO)
                        .filter(d -> !nf.containsKey(d.agendamento().getId())).toList()));
        itens.add(item("DIVERGENCIAS", "Divergências registradas no dia", "aviso",
                doDia.stream().filter(d -> d.agendamento().divergente()).toList()));
        List<ValePalete> pendentes = vales.listar(null, dia, ValePalete.Status.PENDENTE, null);
        itens.add(new ItemRotina("VALES", "Vales-palete pendentes de assinatura", "aviso", pendentes.stream()
                .map(v -> new Referencia(v.getId(), v.getCodigo() + " · " + v.getFornecedor(), "/vales-palete/" + v.getId()))
                .toList()));
        List<CobrancaPalete> vencidas = cobrancas.listar(null, CobrancaPalete.Status.ABERTA).stream()
                .filter(c -> c.vencida(hoje)).toList();
        itens.add(new ItemRotina("COBRANCAS", "Cobranças de paletes vencidas", "erro", vencidas.stream()
                .map(c -> new Referencia(c.getId(), c.getFornecedor() + " · " + c.getQuantidade() + " paletes", "/paletes/cobrancas"))
                .toList()));
        return new Rotina(dia, itens);
    }

    private Map<Long, List<NotaFiscal>> notasPor(List<AgendamentoDetalhe> lista) {
        List<Long> ids = lista.stream().map(d -> d.agendamento().getId()).toList();
        if (ids.isEmpty()) return Map.of();
        return notas.listarPorAgendamentos(ids).stream().collect(Collectors.groupingBy(NotaFiscal::agendamentoId));
    }

    private static ItemRotina item(String codigo, String titulo, String nivel, List<AgendamentoDetalhe> lista) {
        return new ItemRotina(codigo, titulo, nivel, lista.stream().map(d -> new Referencia(d.agendamento().getId(),
                d.faixaDescricao() + " · " + d.agendamento().getFornecedor() + " · pedido " + d.agendamento().getIdPedido(),
                "/agendamentos/" + d.agendamento().getId())).toList());
    }

    private static double media(List<Long> v) {
        return v.stream().filter(java.util.Objects::nonNull).mapToLong(Long::longValue).average().orElse(0);
    }

    private static void exigirPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) throw new RegraNegocioException("Informe início e fim do período");
    }

    static Set<String> codigos(Rotina r) {
        return r.itens().stream().map(ItemRotina::codigo).collect(Collectors.toSet());
    }
}
