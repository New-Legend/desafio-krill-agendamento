package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.application.port.CobrancaPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.MovimentoPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.application.port.ValePaleteRepositorio;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.model.CobrancaPalete;
import br.com.redekrill.agendamento.domain.model.MovimentoPalete;
import br.com.redekrill.agendamento.domain.model.ValePalete;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Conta-corrente de paletes: saldo por fornecedor (vales − cobranças), cobrança e recibos,
 * e movimentos/saldo por loja (romaneios de envio e retorno).
 */
public class PaleteFinanceiroUseCase {

    public record SaldoFornecedor(String fornecedor, int vales, int entregues, int devolvidos, int cobrados, int pagos,
                                  int saldoEmAberto, BigDecimal valorEmAberto) {
    }

    /** porTipo: saldo por código de tipo de palete (pode ser negativo se a loja devolveu mais do que recebeu). */
    public record SaldoLoja(String loja, Map<String, Integer> porTipo, int total, LocalDate ultimoMovimento) {
    }

    public record NovaCobranca(String fornecedor, Integer quantidade, BigDecimal valorUnitario, LocalDate vencimento,
                               String observacao) {
    }

    public record NovoMovimento(LocalDate data, MovimentoPalete.Tipo tipo, Long cdId, String loja,
                                ValePalete.Quantidades quantidades, String observacao) {
    }

    private static final DateTimeFormatter AAAAMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ValePaleteRepositorio vales;
    private final CobrancaPaleteRepositorio cobrancas;
    private final MovimentoPaleteRepositorio movimentos;
    private final CentroDistribuicaoRepositorio cds;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;
    private final Clock clock;
    private final TipoPaleteUseCase tipos;

    public PaleteFinanceiroUseCase(ValePaleteRepositorio vales, CobrancaPaleteRepositorio cobrancas,
                                   MovimentoPaleteRepositorio movimentos, CentroDistribuicaoRepositorio cds,
                                   AuditoriaUseCase auditoria, Transacao transacao, Clock clock) {
        this(vales, cobrancas, movimentos, cds, auditoria, transacao, clock, null);
    }

    public PaleteFinanceiroUseCase(ValePaleteRepositorio vales, CobrancaPaleteRepositorio cobrancas,
                                   MovimentoPaleteRepositorio movimentos, CentroDistribuicaoRepositorio cds,
                                   AuditoriaUseCase auditoria, Transacao transacao, Clock clock, TipoPaleteUseCase tipos) {
        this.tipos = tipos;
        this.vales = vales;
        this.cobrancas = cobrancas;
        this.movimentos = movimentos;
        this.cds = cds;
        this.auditoria = auditoria;
        this.transacao = transacao;
        this.clock = clock;
    }

    // ---- Conta-corrente por fornecedor
    public List<SaldoFornecedor> saldoPorFornecedor(String busca) {
        Map<String, int[]> mapa = new TreeMap<>();
        Map<String, String> nomes = new LinkedHashMap<>();
        Map<String, BigDecimal> aberto = new LinkedHashMap<>();
        for (ValePalete v : vales.listar(null, null, null, busca)) {
            if (v.getStatus() == ValePalete.Status.CANCELADO) continue;
            String k = chave(v.getFornecedor());
            nomes.putIfAbsent(k, v.getFornecedor());
            int[] s = mapa.computeIfAbsent(k, x -> new int[5]);
            s[0]++;
            s[1] += v.getEntregues().total();
            s[2] += v.getDevolvidos().total();
        }
        for (CobrancaPalete c : cobrancas.listar(busca, null)) {
            if (!c.ativa()) continue;
            String k = chave(c.getFornecedor());
            nomes.putIfAbsent(k, c.getFornecedor());
            int[] s = mapa.computeIfAbsent(k, x -> new int[5]);
            s[3] += c.getQuantidade();
            if (c.getStatus() == CobrancaPalete.Status.PAGA) s[4] += c.getQuantidade();
            else aberto.merge(k, c.valorTotal(), BigDecimal::add);
        }
        return mapa.entrySet().stream().map(e -> {
            int[] s = e.getValue();
            return new SaldoFornecedor(nomes.get(e.getKey()), s[0], s[1], s[2], s[3], s[4], Math.max(0, s[1] - s[2] - s[3]),
                    aberto.getOrDefault(e.getKey(), BigDecimal.ZERO));
        }).sorted(Comparator.comparingInt(SaldoFornecedor::saldoEmAberto).reversed()).toList();
    }

    public int saldoEmAberto(String fornecedor) {
        String k = chave(fornecedor);
        return saldoPorFornecedor(fornecedor).stream().filter(s -> chave(s.fornecedor()).equals(k))
                .mapToInt(SaldoFornecedor::saldoEmAberto).sum();
    }

    // ---- Cobranças e recibos
    public List<CobrancaPalete> listarCobrancas(String fornecedor, CobrancaPalete.Status status) {
        return cobrancas.listar(fornecedor, status);
    }

    public CobrancaPalete cobranca(Long id) {
        return cobrancas.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cobrança", id));
    }

    public CobrancaPalete cobrar(NovaCobranca cmd, Solicitante s) {
        return transacao.executar(() -> {
            int qtd = cmd.quantidade() == null ? 0 : cmd.quantidade();
            CobrancaPalete c = CobrancaPalete.nova(cmd.fornecedor(), qtd, cmd.valorUnitario(), cmd.vencimento(),
                    cmd.observacao(), saldoEmAberto(cmd.fornecedor()), s.usuario(), OffsetDateTime.now(clock));
            CobrancaPalete salva = cobrancas.salvar(c);
            auditoria.registrar(s, "CRIAR", "COBRANCA_PALETE", salva.getId(), Map.of("fornecedor", salva.getFornecedor(),
                    "quantidade", salva.getQuantidade(), "valor", salva.valorTotal().toPlainString()));
            return salva;
        });
    }

    public CobrancaPalete pagar(Long id, CobrancaPalete.FormaPagamento forma, Solicitante s) {
        return transacao.executar(() -> {
            CobrancaPalete c = cobranca(id);
            OffsetDateTime agora = OffsetDateTime.now(clock);
            String recibo = "RP-" + agora.format(AAAAMMDD) + "-" + String.format("%04d", cobrancas.proximoRecibo());
            c.pagar(forma, recibo, s.usuario(), agora);
            CobrancaPalete salva = cobrancas.salvar(c);
            auditoria.registrar(s, "PAGAR", "COBRANCA_PALETE", id, Map.of("recibo", recibo, "forma", forma.name()));
            return salva;
        });
    }

    public CobrancaPalete cancelarCobranca(Long id, String motivo, Solicitante s) {
        return transacao.executar(() -> {
            CobrancaPalete c = cobranca(id);
            c.cancelar(motivo);
            CobrancaPalete salva = cobrancas.salvar(c);
            auditoria.registrar(s, "CANCELAR", "COBRANCA_PALETE", id, Map.of("motivo", salva.getMotivoCancelamento()));
            return salva;
        });
    }

    // ---- Movimentos entre CD e lojas
    public MovimentoPalete registrarMovimento(NovoMovimento cmd, Solicitante s) {
        return transacao.executar(() -> {
            if (tipos != null && cmd.quantidades() != null) tipos.validarNovo(cmd.quantidades());
            Long cd = cmd.cdId() == null ? 1L : cmd.cdId();
            cds.buscarPorId(cd).orElseThrow(() -> new RecursoNaoEncontradoException("CD", cd));
            LocalDate data = cmd.data() == null ? LocalDate.now(clock) : cmd.data();
            String romaneio = "RM-" + data.format(AAAAMMDD) + "-" + String.format("%04d", movimentos.proximoRomaneio());
            MovimentoPalete m = movimentos.salvar(new MovimentoPalete(null, data, cmd.tipo(), cd, cmd.loja(), romaneio,
                    cmd.quantidades(), cmd.observacao(), s.usuario(), OffsetDateTime.now(clock)));
            auditoria.registrar(s, cmd.tipo().name(), "MOVIMENTO_PALETE", m.id(), Map.of("loja", m.loja(),
                    "romaneio", romaneio, "total", m.quantidades().total()));
            return m;
        });
    }

    public List<MovimentoPalete> listarMovimentos(String loja, LocalDate inicio, LocalDate fim) {
        return movimentos.listar(loja, inicio, fim);
    }

    /** Saldo de paletes que cada loja deve ao CD (visão da loja). */
    public List<SaldoLoja> saldoPorLoja(String loja) {
        Map<String, SaldoLoja> mapa = new TreeMap<>();
        for (MovimentoPalete m : movimentos.listar(loja, null, null)) {
            String k = chave(m.loja());
            SaldoLoja a = mapa.getOrDefault(k, new SaldoLoja(m.loja(), new TreeMap<>(), 0, null));
            LocalDate ultimo = a.ultimoMovimento() == null || m.data().isAfter(a.ultimoMovimento()) ? m.data() : a.ultimoMovimento();
            Map<String, Integer> porTipo = new TreeMap<>(a.porTipo());
            m.quantidades().tipos().forEach(t -> porTipo.merge(t, m.de(t), Integer::sum));
            mapa.put(k, new SaldoLoja(a.loja(), porTipo, a.total() + m.total(), ultimo));
        }
        return List.copyOf(mapa.values());
    }

    private static String chave(String nome) {
        return nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
    }
}
