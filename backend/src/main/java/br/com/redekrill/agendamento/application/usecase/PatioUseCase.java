package br.com.redekrill.agendamento.application.usecase;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.application.port.DocaRepositorio;
import br.com.redekrill.agendamento.application.port.LogisticaRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.CalculadoraTaxaDescarga;
import br.com.redekrill.agendamento.domain.model.Doca;
import br.com.redekrill.agendamento.domain.model.LogisticaAgendamento;
import br.com.redekrill.agendamento.domain.model.OcupacaoRecebimento;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pátio e docas do recebimento (porte de /yms/docas e /yms/quadro do Gestão CD): cadastro de docas por CD,
 * quadro do pátio (previstos → aguardando doca → na doca → descarregando → liberados), atribuição de doca e
 * indicador de lotação do CD agora (docas) e no dia (capacidade diária).
 */
public class PatioUseCase {

    public enum Coluna { PREVISTO, AGUARDANDO, NA_DOCA, DESCARREGANDO, LIBERADO }

    public record DadosDoca(Long cdId, String codigo, String nome, Boolean ativa) {
    }

    public record Cartao(Long agendamentoId, String fornecedor, String transportadora, String loja, String placa,
                         String status, String faixa, String tipoCaminhao, int paletes, Long docaId, String doca,
                         OffsetDateTime chegadaEm, OffsetDateTime docaEm, String modal, BigDecimal taxaDescarga) {
    }

    public record SituacaoDoca(Doca doca, Long agendamentoId, String placa, String fornecedor) {
        public boolean ocupada() {
            return agendamentoId != null;
        }
    }

    public record Indicador(int docasAtivas, int docasOcupadas, boolean lotadoAgora, int caminhoesNoPatio,
                            OcupacaoRecebimento dia, boolean diaCheio) {
    }

    public record Quadro(LocalDate data, Long cdId, List<SituacaoDoca> docas, Map<Coluna, List<Cartao>> colunas,
                         Indicador indicador) {
    }

    private final DocaRepositorio docas;
    private final LogisticaRepositorio logistica;
    private final AgendamentoRepositorio agendamentos;
    private final AgendamentoUseCase agendamentoUseCase;
    private final CentroDistribuicaoRepositorio cds;
    private final CapacidadeRecebimentoUseCase capacidade;
    private final TaxaDescargaUseCase taxas;
    private final AuditoriaUseCase auditoria;
    private final Transacao transacao;
    private final Clock clock;

    public PatioUseCase(DocaRepositorio docas, LogisticaRepositorio logistica, AgendamentoRepositorio agendamentos,
                        AgendamentoUseCase agendamentoUseCase, CentroDistribuicaoRepositorio cds,
                        CapacidadeRecebimentoUseCase capacidade, TaxaDescargaUseCase taxas, AuditoriaUseCase auditoria,
                        Transacao transacao, Clock clock) {
        this.docas = docas;
        this.logistica = logistica;
        this.agendamentos = agendamentos;
        this.agendamentoUseCase = agendamentoUseCase;
        this.cds = cds;
        this.capacidade = capacidade;
        this.taxas = taxas;
        this.auditoria = auditoria;
        this.transacao = transacao;
        this.clock = clock;
    }

    // ---- Cadastro de docas
    public List<Doca> listarDocas(Long cdId) {
        return docas.listar(cdId);
    }

    public Doca salvarDoca(Long id, DadosDoca d, Solicitante s) {
        return transacao.executar(() -> {
            Long cd = d.cdId() == null ? 1L : d.cdId();
            cds.buscarPorId(cd).orElseThrow(() -> new RecursoNaoEncontradoException("CD", cd));
            if (id != null) docas.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Doca", id));
            Doca doca = new Doca(id, cd, d.codigo(), d.nome(), d.ativa() == null || d.ativa());
            if (docas.existeCodigo(cd, doca.codigo(), id)) throw new RegraNegocioException("Já existe a doca " + doca.codigo() + " neste CD");
            if (id != null && !doca.ativa() && logistica.ocupanteDaDoca(id, null).isPresent()) {
                throw new RegraNegocioException("Libere a doca antes de desativá-la");
            }
            Doca salva = docas.salvar(doca);
            auditoria.registrar(s, id == null ? "CRIAR" : "ALTERAR", "DOCA", salva.id(),
                    Map.of("codigo", salva.codigo(), "cdId", cd, "ativa", salva.ativa()));
            return salva;
        });
    }

    // ---- Dados logísticos do agendamento
    public LogisticaAgendamento logistica(Long agendamentoId) {
        return logistica.buscar(agendamentoId).orElse(LogisticaAgendamento.vazia(agendamentoId));
    }

    /** Grava loja (entrega direta), transportadora e modal; devolve os alertas de capacidade do destino. */
    public List<String> registrarLogistica(Long agendamentoId, String loja, String transportadora, String modal, Solicitante s) {
        return transacao.executar(() -> {
            Agendamento a = agendamentos.buscarPorId(agendamentoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", agendamentoId));
            LogisticaAgendamento nova = logistica(agendamentoId).comDados(loja, transportadora, modal);
            logistica.salvar(nova);
            Map<String, Object> det = new HashMap<>();
            det.put("loja", nova.loja() == null ? "CD" : nova.loja());
            det.put("transportadora", nova.transportadora() == null ? "-" : nova.transportadora());
            auditoria.registrar(s, "LOGISTICA", "AGENDAMENTO", agendamentoId, det);
            return capacidade.ocupacao(a.getData(), a.getCdId(), nova.loja()).alertas();
        });
    }

    /** Atribui (ou libera, com docaId nulo) a doca de um caminhão que já chegou. */
    public LogisticaAgendamento atribuirDoca(Long agendamentoId, Long docaId, Solicitante s) {
        return transacao.executar(() -> {
            Agendamento a = agendamentos.buscarPorId(agendamentoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", agendamentoId));
            if (docaId != null) {
                if (a.getStatus() != StatusAgendamento.CHEGOU && a.getStatus() != StatusAgendamento.EM_RECEBIMENTO) {
                    throw new RegraNegocioException("Só é possível atribuir doca a caminhão que chegou ou está em recebimento");
                }
                Doca d = docas.buscarPorId(docaId).orElseThrow(() -> new RecursoNaoEncontradoException("Doca", docaId));
                if (!d.ativa()) throw new RegraNegocioException("Doca " + d.nome() + " está inativa");
                Long cd = a.getCdId() == null ? 1L : a.getCdId();
                if (!d.cdId().equals(cd)) throw new RegraNegocioException("A doca " + d.nome() + " é de outro CD");
                logistica.ocupanteDaDoca(docaId, agendamentoId).ifPresent(outro -> {
                    throw new RegraNegocioException("Doca " + d.nome() + " ocupada pelo agendamento #" + outro);
                });
            }
            LogisticaAgendamento salva = logistica.salvar(logistica(agendamentoId).comDoca(docaId, OffsetDateTime.now(clock)));
            Map<String, Object> det = new HashMap<>();
            det.put("doca", docaId == null ? "liberada" : docaId);
            auditoria.registrar(s, docaId == null ? "LIBERAR_DOCA" : "ATRIBUIR_DOCA", "AGENDAMENTO", agendamentoId, det);
            return salva;
        });
    }

    // ---- Quadro do pátio
    public Quadro quadro(LocalDate data, Long cdId) {
        LocalDate dia = data == null ? LocalDate.now(clock) : data;
        Long cd = cdId == null ? 1L : cdId;
        List<AgendamentoDetalhe> lista = agendamentoUseCase.listar(new FiltroAgendamento(dia, dia, null, null, null, null, cd, false));
        Map<Long, LogisticaAgendamento> log = logistica.porAgendamentos(lista.stream().map(d -> d.agendamento().getId()).toList());
        List<Doca> docasCd = docas.listar(cd).stream().filter(Doca::ativa).toList();
        Map<Long, Doca> docaPorId = docas.listar(cd).stream().collect(Collectors.toMap(Doca::id, Function.identity()));

        Map<Coluna, List<Cartao>> colunas = new java.util.EnumMap<>(Coluna.class);
        for (Coluna c : Coluna.values()) colunas.put(c, new ArrayList<>());
        Map<Long, Cartao> naDoca = new HashMap<>();
        for (AgendamentoDetalhe det : lista) {
            Agendamento a = det.agendamento();
            LogisticaAgendamento l = log.getOrDefault(a.getId(), LogisticaAgendamento.vazia(a.getId()));
            Coluna col = coluna(a.getStatus(), l.docaId() != null);
            if (col == null) continue;
            Cartao cartao = cartao(det, l, docaPorId.get(l.docaId()));
            colunas.get(col).add(cartao);
            if (l.docaId() != null && (col == Coluna.NA_DOCA || col == Coluna.DESCARREGANDO)) naDoca.put(l.docaId(), cartao);
        }
        List<SituacaoDoca> situacoes = docasCd.stream().map(d -> {
            Cartao c = naDoca.get(d.id());
            return new SituacaoDoca(d, c == null ? null : c.agendamentoId(), c == null ? null : c.placa(), c == null ? null : c.fornecedor());
        }).toList();
        int ocupadas = (int) situacoes.stream().filter(SituacaoDoca::ocupada).count();
        int noPatio = colunas.get(Coluna.AGUARDANDO).size() + colunas.get(Coluna.NA_DOCA).size() + colunas.get(Coluna.DESCARREGANDO).size();
        OcupacaoRecebimento ocupacaoDia = capacidade.ocupacao(dia, cd, null);
        Indicador ind = new Indicador(docasCd.size(), ocupadas, !docasCd.isEmpty() && ocupadas >= docasCd.size(), noPatio,
                ocupacaoDia, ocupacaoDia.cheio());
        return new Quadro(dia, cd, situacoes, colunas, ind);
    }

    static Coluna coluna(StatusAgendamento status, boolean comDoca) {
        return switch (status) {
            case AGENDADO, CONFIRMADO -> Coluna.PREVISTO;
            case CHEGOU -> comDoca ? Coluna.NA_DOCA : Coluna.AGUARDANDO;
            case EM_RECEBIMENTO -> Coluna.DESCARREGANDO;
            case RECEBIDO -> Coluna.LIBERADO;
            default -> null;
        };
    }

    private Cartao cartao(AgendamentoDetalhe det, LogisticaAgendamento l, Doca doca) {
        Agendamento a = det.agendamento();
        int paletes = a.getPaletesRecebidos() == null ? a.getQuantidadePaletes() : a.getPaletesRecebidos();
        var cobranca = taxas.calcular(l.modalDescarga(), det.tipoCaminhao(), paletes);
        return new Cartao(a.getId(), a.getFornecedor(), l.transportadora(), l.loja(), a.getPlaca(), a.getStatus().rotulo(),
                det.faixaDescricao(), det.tipoCaminhao(), paletes, l.docaId(), doca == null ? null : doca.nome(),
                a.getChegadaEm(), l.docaEm(),
                cobranca.map(CalculadoraTaxaDescarga.Cobranca::nomeModal).orElse(null),
                cobranca.map(CalculadoraTaxaDescarga.Cobranca::valor).orElse(null));
    }
}
