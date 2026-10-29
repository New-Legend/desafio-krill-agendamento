package br.com.redekrill.agendamento.application;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.AuditoriaRepositorio;
import br.com.redekrill.agendamento.application.port.CapacidadeRecebimentoRepositorio;
import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.application.port.ConfiguracaoRepositorio;
import br.com.redekrill.agendamento.application.port.DocaRepositorio;
import br.com.redekrill.agendamento.application.port.ItemAvulsoRepositorio;
import br.com.redekrill.agendamento.application.port.LogisticaRepositorio;
import br.com.redekrill.agendamento.application.port.ParametroRepositorio;
import br.com.redekrill.agendamento.application.port.ProdutoRepositorio;
import br.com.redekrill.agendamento.application.port.TaxaDescargaRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.application.port.ValePaleteRepositorio;
import br.com.redekrill.agendamento.application.usecase.AgendamentoUseCase;
import br.com.redekrill.agendamento.application.usecase.AuditoriaUseCase;
import br.com.redekrill.agendamento.application.usecase.CapacidadeRecebimentoUseCase;
import br.com.redekrill.agendamento.application.usecase.ConfiguracaoValePaleteUseCase;
import br.com.redekrill.agendamento.application.usecase.ItemAvulsoUseCase;
import br.com.redekrill.agendamento.application.usecase.PatioUseCase;
import br.com.redekrill.agendamento.application.usecase.TaxaDescargaUseCase;
import br.com.redekrill.agendamento.application.usecase.ValePaleteUseCase;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.CalculadoraTaxaDescarga;
import br.com.redekrill.agendamento.domain.model.CapacidadeRecebimento;
import br.com.redekrill.agendamento.domain.model.CentroDistribuicao;
import br.com.redekrill.agendamento.domain.model.Doca;
import br.com.redekrill.agendamento.domain.model.ItemAvulso;
import br.com.redekrill.agendamento.domain.model.LogisticaAgendamento;
import br.com.redekrill.agendamento.domain.model.OcupacaoRecebimento;
import br.com.redekrill.agendamento.domain.model.ParametroCapacidade;
import br.com.redekrill.agendamento.domain.model.Produto;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import br.com.redekrill.agendamento.domain.model.TaxaDescarga;
import br.com.redekrill.agendamento.domain.model.TipoPalete;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import br.com.redekrill.agendamento.domain.service.CatalogoProdutoCsv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pátio e docas, capacidade diária por CD/loja, taxa de descarga por modal, produto avulso e configurações do vale. */
class PatioCapacidadeTaxasTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T14:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 2);
    static final Solicitante ADMIN = new Solicitante("admin", "10.0.0.1");
    static final Transacao TX = new Transacao() {
        @Override
        public <T> T executar(Supplier<T> op) {
            return op.get();
        }
    };

    // ---- Fakes em memória
    static class DocasMem implements DocaRepositorio {
        final List<Doca> itens = new ArrayList<>();
        public List<Doca> listar(Long cd) { return itens.stream().filter(d -> cd == null || d.cdId().equals(cd)).toList(); }
        public Optional<Doca> buscarPorId(Long id) { return itens.stream().filter(d -> d.id().equals(id)).findFirst(); }
        public boolean existeCodigo(Long cd, String c, Long ign) {
            return itens.stream().anyMatch(d -> d.cdId().equals(cd) && d.codigo().equalsIgnoreCase(c) && !d.id().equals(ign));
        }
        public Doca salvar(Doca d) {
            Doca s = d.id() == null ? new Doca((long) itens.size() + 1, d.cdId(), d.codigo(), d.nome(), d.ativa()) : d;
            itens.removeIf(x -> x.id().equals(s.id()));
            itens.add(s);
            return s;
        }
    }

    static class LogisticaMem implements LogisticaRepositorio {
        final Map<Long, LogisticaAgendamento> itens = new HashMap<>();
        final Map<Long, StatusAgendamento> status = new HashMap<>();
        Consumo consumo = new Consumo(0, 0);
        public Optional<LogisticaAgendamento> buscar(Long id) { return Optional.ofNullable(itens.get(id)); }
        public Map<Long, LogisticaAgendamento> porAgendamentos(Collection<Long> ids) {
            Map<Long, LogisticaAgendamento> r = new HashMap<>();
            ids.forEach(i -> { if (itens.containsKey(i)) r.put(i, itens.get(i)); });
            return r;
        }
        public LogisticaAgendamento salvar(LogisticaAgendamento l) { itens.put(l.agendamentoId(), l); return l; }
        public Optional<Long> ocupanteDaDoca(Long doca, Long ign) {
            return itens.values().stream().filter(l -> doca.equals(l.docaId()) && !l.agendamentoId().equals(ign))
                    .filter(l -> status.getOrDefault(l.agendamentoId(), StatusAgendamento.CHEGOU) == StatusAgendamento.CHEGOU
                            || status.get(l.agendamentoId()) == StatusAgendamento.EM_RECEBIMENTO)
                    .map(LogisticaAgendamento::agendamentoId).findFirst();
        }
        public Consumo consumo(LocalDate d, Long cd, String loja) { return consumo; }
        public List<SaldoTransportadora> saldosPorTransportadora() { return List.of(); }
    }

    static class CapacidadesMem implements CapacidadeRecebimentoRepositorio {
        final List<CapacidadeRecebimento> itens = new ArrayList<>();
        public List<CapacidadeRecebimento> listar() { return List.copyOf(itens); }
        public Optional<CapacidadeRecebimento> buscarPorId(Long id) { return itens.stream().filter(c -> c.id().equals(id)).findFirst(); }
        public CapacidadeRecebimento salvar(CapacidadeRecebimento c) {
            CapacidadeRecebimento s = c.id() == null
                    ? new CapacidadeRecebimento((long) itens.size() + 1, c.cdId(), c.loja(), c.diaSemana(), c.paletes(), c.caminhoes()) : c;
            itens.removeIf(x -> x.id().equals(s.id()));
            itens.add(s);
            return s;
        }
        public void excluir(Long id) { itens.removeIf(c -> c.id().equals(id)); }
    }

    static class TaxasMem implements TaxaDescargaRepositorio {
        final List<TaxaDescarga> itens = new ArrayList<>();
        public List<TaxaDescarga> listar() { return List.copyOf(itens); }
        public Optional<TaxaDescarga> buscarPorId(Long id) { return itens.stream().filter(t -> t.id().equals(id)).findFirst(); }
        public boolean existeModal(String m, Long ign) { return itens.stream().anyMatch(t -> t.modal().equals(m) && !t.id().equals(ign)); }
        public TaxaDescarga salvar(TaxaDescarga t) {
            TaxaDescarga s = t.id() == null ? new TaxaDescarga((long) itens.size() + 1, t.modal(), t.nome(), t.valorPorPalete(), t.ativo()) : t;
            itens.removeIf(x -> x.id().equals(s.id()));
            itens.add(s);
            return s;
        }
    }

    final AuditoriaUseCase auditoria = new AuditoriaUseCase(mock(AuditoriaRepositorio.class), CLOCK);
    final CentroDistribuicaoRepositorio cds = mock(CentroDistribuicaoRepositorio.class);
    final ParametroRepositorio parametros = mock(ParametroRepositorio.class);
    final AgendamentoRepositorio agendamentos = mock(AgendamentoRepositorio.class);
    final AgendamentoUseCase agendamentoUseCase = mock(AgendamentoUseCase.class);
    DocasMem docas;
    LogisticaMem logistica;
    CapacidadesMem capacidades;
    TaxasMem taxasRepo;
    CapacidadeRecebimentoUseCase capacidade;
    TaxaDescargaUseCase taxas;
    PatioUseCase patio;

    @BeforeEach
    void setUp() {
        docas = new DocasMem();
        logistica = new LogisticaMem();
        capacidades = new CapacidadesMem();
        taxasRepo = new TaxasMem();
        when(cds.buscarPorId(1L)).thenReturn(Optional.of(new CentroDistribuicao(1L, "CD1", "CD Principal", null, null, null, true)));
        when(cds.buscarPorId(2L)).thenReturn(Optional.of(new CentroDistribuicao(2L, "CD2", "CD Norte", null, 200, null, true)));
        when(parametros.obter()).thenReturn(ParametroCapacidade.PADRAO);
        capacidade = new CapacidadeRecebimentoUseCase(capacidades, logistica, cds, parametros, auditoria, TX);
        taxas = new TaxaDescargaUseCase(taxasRepo, auditoria, TX);
        taxas.salvar(null, new TaxaDescargaUseCase.Dados("SIDER", "Sider / carreta", new BigDecimal("25"), true), ADMIN);
        taxas.salvar(null, new TaxaDescargaUseCase.Dados("bau", "Baú", new BigDecimal("30"), null), ADMIN);
        taxas.salvar(null, new TaxaDescargaUseCase.Dados("Granel", "Granel", new BigDecimal("60"), true), ADMIN);
        patio = new PatioUseCase(docas, logistica, agendamentos, agendamentoUseCase, cds, capacidade, taxas, auditoria, TX, CLOCK);
    }

    Agendamento agendamento(long id, StatusAgendamento status, Long cd) {
        Agendamento a = mock(Agendamento.class);
        when(a.getId()).thenReturn(id);
        when(a.getStatus()).thenReturn(status);
        when(a.getCdId()).thenReturn(cd);
        when(a.getData()).thenReturn(SEGUNDA);
        when(a.getFornecedor()).thenReturn("Fornecedor " + id);
        when(a.getPlaca()).thenReturn("ABC1D2" + (id % 10));
        when(a.getQuantidadePaletes()).thenReturn(10);
        when(a.getPaletesRecebidos()).thenReturn(null);
        when(agendamentos.buscarPorId(id)).thenReturn(Optional.of(a));
        logistica.status.put(id, status);
        return a;
    }

    // ---- Domínio
    @Test
    void capacidadeDoDiaDaSemanaSobrepoeRegraGeral() {
        CapacidadeRecebimento geral = new CapacidadeRecebimento(1L, 1L, null, null, 400, null);
        CapacidadeRecebimento segunda = new CapacidadeRecebimento(2L, 1L, null, 1, 300, 12);
        assertThat(CapacidadeRecebimento.resolver(List.of(geral, segunda), SEGUNDA)).contains(segunda);
        assertThat(CapacidadeRecebimento.resolver(List.of(geral, segunda), SEGUNDA.plusDays(1))).contains(geral);
        assertThat(CapacidadeRecebimento.resolver(List.of(segunda), SEGUNDA.plusDays(1))).isEmpty();
        assertThatThrownBy(() -> new CapacidadeRecebimento(null, 1L, "Loja 1", null, 10, null)).hasMessageContaining("apenas um");
        assertThatThrownBy(() -> new CapacidadeRecebimento(null, null, " ", null, 10, null)).hasMessageContaining("apenas um");
        assertThatThrownBy(() -> new CapacidadeRecebimento(null, 1L, null, 8, 10, null)).hasMessageContaining("Dia da semana");
        assertThatThrownBy(() -> new CapacidadeRecebimento(null, 1L, null, 0, 10, null)).hasMessageContaining("Dia da semana");
        assertThatThrownBy(() -> new CapacidadeRecebimento(null, 1L, null, null, null, null)).hasMessageContaining("paletes e/ou");
        assertThatThrownBy(() -> new CapacidadeRecebimento(null, 1L, null, null, 0, null)).hasMessageContaining("paletes deve");
        assertThatThrownBy(() -> new CapacidadeRecebimento(null, 1L, null, null, null, 0)).hasMessageContaining("caminhões deve");
    }

    @Test
    void ocupacaoRecebimentoCalculaPercentualCheioEAlertas() {
        OcupacaoRecebimento o = new OcupacaoRecebimento(SEGUNDA, "o CD CD1", 100, 120, 5, 4);
        assertThat(o.percentualPaletes()).isEqualTo(120.0);
        assertThat(o.percentualCaminhoes()).isEqualTo(80.0);
        assertThat(o.percentual()).isEqualTo(120.0);
        assertThat(o.cheio()).isTrue();
        assertThat(o.alertas()).hasSize(1).first().asString().contains("120 de 100");
        OcupacaoRecebimento caminhoes = new OcupacaoRecebimento(SEGUNDA, "a loja L1", null, 50, 2, 3);
        assertThat(caminhoes.alertas()).singleElement().asString().contains("caminhões");
        OcupacaoRecebimento livre = new OcupacaoRecebimento(SEGUNDA, "a loja L2", null, 50, null, 9);
        assertThat(livre.limitado()).isFalse();
        assertThat(livre.cheio()).isFalse();
        assertThat(livre.percentual()).isZero();
        assertThat(livre.alertas()).isEmpty();
    }

    @Test
    void taxaDeDescargaPorModalDeduzidaDoCaminhao() {
        assertThat(CalculadoraTaxaDescarga.deduzirModal("Carreta sider")).isEqualTo("SIDER");
        assertThat(CalculadoraTaxaDescarga.deduzirModal("Baú refrigerado")).isEqualTo("BAU");
        assertThat(CalculadoraTaxaDescarga.deduzirModal("Granel a rebater")).isEqualTo("GRANEL");
        assertThat(CalculadoraTaxaDescarga.deduzirModal(null)).isEqualTo("SIDER");
        var bau = taxas.calcular(null, "Truck baú", 10).orElseThrow();
        assertThat(bau.modal()).isEqualTo("BAU");
        assertThat(bau.valor()).isEqualByComparingTo("300.00");
        assertThat(taxas.calcular("granel", "Carreta", 3).orElseThrow().valor()).isEqualByComparingTo("180.00");
        assertThat(taxas.calcular(null, "Carreta", -1).orElseThrow().valor()).isEqualByComparingTo("0");
        assertThat(taxas.calcular("FRIGORIFICO", "Carreta", 3)).isEmpty();
        assertThatThrownBy(() -> new TaxaDescarga(null, "x", "X", BigDecimal.ONE, true)).hasMessageContaining("Modal");
        assertThatThrownBy(() -> new TaxaDescarga(null, "BAU", "Baú", new BigDecimal("-1"), true)).hasMessageContaining("Taxa");
        assertThatThrownBy(() -> new TaxaDescarga(null, "BAU", "Baú", null, true)).hasMessageContaining("Taxa");
    }

    @Test
    void cadastroDeTaxaNaoTrocaModalNemDuplica() {
        TaxaDescarga bau = taxasRepo.itens.get(1);
        TaxaDescarga alterada = taxas.salvar(bau.id(), new TaxaDescargaUseCase.Dados(null, "Baú", new BigDecimal("32"), false), ADMIN);
        assertThat(alterada.modal()).isEqualTo("BAU");
        assertThat(alterada.ativo()).isFalse();
        assertThat(taxas.calcular("BAU", null, 1)).isEmpty();
        assertThat(taxas.salvar(bau.id(), new TaxaDescargaUseCase.Dados("BAU", "Baú", new BigDecimal("33"), true), ADMIN).valorPorPalete())
                .isEqualByComparingTo("33");
        assertThatThrownBy(() -> taxas.salvar(bau.id(), new TaxaDescargaUseCase.Dados("GRANEL", "X", BigDecimal.ONE, true), ADMIN))
                .hasMessageContaining("não pode ser alterado");
        assertThatThrownBy(() -> taxas.salvar(null, new TaxaDescargaUseCase.Dados("SIDER", "X", BigDecimal.ONE, true), ADMIN))
                .hasMessageContaining("Já existe");
        assertThatThrownBy(() -> taxas.salvar(99L, new TaxaDescargaUseCase.Dados(null, "X", BigDecimal.ONE, true), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(taxas.listar()).hasSize(3);
    }

    // ---- Capacidade por CD e por loja
    @Test
    void capacidadeDoCdUsaCadastroOuCapacidadePropriaDoCd() {
        logistica.consumo = new LogisticaRepositorio.Consumo(240, 6);
        OcupacaoRecebimento semRegra = capacidade.ocupacao(SEGUNDA, null, null);
        assertThat(semRegra.capacidadePaletes()).isEqualTo(480);
        assertThat(semRegra.percentual()).isEqualTo(50.0);
        assertThat(capacidade.ocupacao(SEGUNDA, 2L, " ").capacidadePaletes()).isEqualTo(200);

        capacidade.salvar(null, new CapacidadeRecebimentoUseCase.Dados(1L, null, 1, null, 6), ADMIN);
        OcupacaoRecebimento segunda = capacidade.ocupacao(SEGUNDA, 1L, null);
        assertThat(segunda.capacidadeCaminhoes()).isEqualTo(6);
        assertThat(segunda.capacidadePaletes()).isEqualTo(480);
        assertThat(segunda.cheio()).isTrue();
        assertThat(segunda.destino()).isEqualTo("o CD CD1");
        assertThatThrownBy(() -> capacidade.ocupacao(null, 1L, null)).hasMessageContaining("Data");
    }

    @Test
    void capacidadeDaLojaParaEntregaDireta() {
        logistica.consumo = new LogisticaRepositorio.Consumo(30, 2);
        assertThat(capacidade.ocupacao(SEGUNDA, 1L, "Loja Centro").limitado()).isFalse();
        CapacidadeRecebimento c = capacidade.salvar(null, new CapacidadeRecebimentoUseCase.Dados(1L, " Loja Centro ", null, 24, 2), ADMIN);
        assertThat(c.cdId()).as("loja informada ignora o CD").isNull();
        OcupacaoRecebimento o = capacidade.ocupacao(SEGUNDA, 1L, "loja centro");
        assertThat(o.percentual()).isEqualTo(125.0);
        assertThat(o.alertas()).singleElement().asString().contains("a loja loja centro");
        assertThatThrownBy(() -> capacidade.salvar(null, new CapacidadeRecebimentoUseCase.Dados(null, "LOJA CENTRO", null, 5, null), ADMIN))
                .hasMessageContaining("Já existe");
        CapacidadeRecebimento alterada = capacidade.salvar(c.id(), new CapacidadeRecebimentoUseCase.Dados(null, "Loja Centro", null, 40, null), ADMIN);
        assertThat(alterada.paletes()).isEqualTo(40);
        assertThat(capacidade.listar()).hasSize(1);
        capacidade.excluir(c.id(), ADMIN);
        assertThat(capacidade.listar()).isEmpty();
        assertThatThrownBy(() -> capacidade.excluir(c.id(), ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> capacidade.salvar(9L, new CapacidadeRecebimentoUseCase.Dados(1L, null, null, 5, null), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> capacidade.salvar(null, new CapacidadeRecebimentoUseCase.Dados(7L, null, null, 5, null), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---- Pátio e docas
    @Test
    void cadastroDeDocasPorCd() {
        Doca d1 = patio.salvarDoca(null, new PatioUseCase.DadosDoca(null, "d01", "Doca 1", null), ADMIN);
        assertThat(d1.codigo()).isEqualTo("D01");
        assertThat(d1.cdId()).isEqualTo(1L);
        assertThat(patio.salvarDoca(null, new PatioUseCase.DadosDoca(2L, "D01", "Doca 1 Norte", true), ADMIN).cdId()).isEqualTo(2L);
        assertThatThrownBy(() -> patio.salvarDoca(null, new PatioUseCase.DadosDoca(1L, "D01", "Outra", true), ADMIN))
                .hasMessageContaining("Já existe");
        assertThatThrownBy(() -> patio.salvarDoca(null, new PatioUseCase.DadosDoca(9L, "D09", "X", true), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> patio.salvarDoca(99L, new PatioUseCase.DadosDoca(1L, "D09", "X", true), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> new Doca(null, null, "D1", "X", true)).hasMessageContaining("CD");
        assertThat(patio.listarDocas(1L)).hasSize(1);
        assertThat(patio.listarDocas(null)).hasSize(2);

        agendamento(5L, StatusAgendamento.CHEGOU, 1L);
        patio.atribuirDoca(5L, d1.id(), ADMIN);
        assertThatThrownBy(() -> patio.salvarDoca(d1.id(), new PatioUseCase.DadosDoca(1L, "D01", "Doca 1", false), ADMIN))
                .hasMessageContaining("Libere a doca");
        patio.atribuirDoca(5L, null, ADMIN);
        assertThat(patio.salvarDoca(d1.id(), new PatioUseCase.DadosDoca(1L, "D01", "Doca 1", false), ADMIN).ativa()).isFalse();
    }

    @Test
    void atribuirDocaValidaStatusCdOcupacaoEDocaInativa() {
        Doca d1 = patio.salvarDoca(null, new PatioUseCase.DadosDoca(1L, "D01", "Doca 1", true), ADMIN);
        Doca norte = patio.salvarDoca(null, new PatioUseCase.DadosDoca(2L, "N01", "Doca Norte", true), ADMIN);
        Doca inativa = patio.salvarDoca(null, new PatioUseCase.DadosDoca(1L, "D02", "Doca 2", false), ADMIN);
        agendamento(1L, StatusAgendamento.AGENDADO, 1L);
        agendamento(2L, StatusAgendamento.CHEGOU, null);
        agendamento(3L, StatusAgendamento.EM_RECEBIMENTO, 1L);

        assertThatThrownBy(() -> patio.atribuirDoca(1L, d1.id(), ADMIN)).hasMessageContaining("chegou");
        assertThatThrownBy(() -> patio.atribuirDoca(2L, norte.id(), ADMIN)).hasMessageContaining("outro CD");
        assertThatThrownBy(() -> patio.atribuirDoca(2L, inativa.id(), ADMIN)).hasMessageContaining("inativa");
        assertThatThrownBy(() -> patio.atribuirDoca(2L, 77L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> patio.atribuirDoca(42L, d1.id(), ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);

        LogisticaAgendamento l = patio.atribuirDoca(2L, d1.id(), ADMIN);
        assertThat(l.docaId()).isEqualTo(d1.id());
        assertThat(l.docaEm()).isEqualTo(OffsetDateTime.now(CLOCK));
        assertThatThrownBy(() -> patio.atribuirDoca(3L, d1.id(), ADMIN)).hasMessageContaining("ocupada pelo agendamento #2");
        assertThat(patio.atribuirDoca(2L, d1.id(), ADMIN).docaId()).as("reatribuir a mesma doca é permitido").isEqualTo(d1.id());
        assertThat(patio.atribuirDoca(2L, null, ADMIN).docaEm()).isNull();
        assertThat(patio.atribuirDoca(3L, d1.id(), ADMIN).docaId()).isEqualTo(d1.id());
    }

    @Test
    void quadroDoPatioSeparaColunasEIndicaLotacao() {
        Doca d1 = patio.salvarDoca(null, new PatioUseCase.DadosDoca(1L, "D01", "Doca 1", true), ADMIN);
        Doca d2 = patio.salvarDoca(null, new PatioUseCase.DadosDoca(1L, "D02", "Doca 2", true), ADMIN);
        patio.salvarDoca(null, new PatioUseCase.DadosDoca(1L, "D03", "Doca 3", false), ADMIN);
        Agendamento previsto = agendamento(1L, StatusAgendamento.CONFIRMADO, 1L);
        Agendamento aguardando = agendamento(2L, StatusAgendamento.CHEGOU, 1L);
        Agendamento naDoca = agendamento(3L, StatusAgendamento.CHEGOU, 1L);
        Agendamento descarregando = agendamento(4L, StatusAgendamento.EM_RECEBIMENTO, 1L);
        Agendamento liberado = agendamento(5L, StatusAgendamento.RECEBIDO, 1L);
        when(liberado.getPaletesRecebidos()).thenReturn(8);
        Agendamento cancelado = agendamento(6L, StatusAgendamento.CANCELADO, 1L);
        patio.atribuirDoca(3L, d1.id(), ADMIN);
        patio.atribuirDoca(4L, d2.id(), ADMIN);
        patio.registrarLogistica(4L, null, "Transportes Sul", "granel", ADMIN);
        when(agendamentoUseCase.listar(any())).thenReturn(List.of(previsto, aguardando, naDoca, descarregando, liberado, cancelado)
                .stream().map(a -> new AgendamentoDetalhe(a, "08:00 - 10:00", "Carreta baú", "Paletizado")).toList());
        logistica.consumo = new LogisticaRepositorio.Consumo(500, 6);

        PatioUseCase.Quadro q = patio.quadro(null, null);
        assertThat(q.data()).isEqualTo(SEGUNDA);
        assertThat(q.colunas().get(PatioUseCase.Coluna.PREVISTO)).extracting(PatioUseCase.Cartao::agendamentoId).containsExactly(1L);
        assertThat(q.colunas().get(PatioUseCase.Coluna.AGUARDANDO)).extracting(PatioUseCase.Cartao::agendamentoId).containsExactly(2L);
        assertThat(q.colunas().get(PatioUseCase.Coluna.NA_DOCA)).singleElement().extracting(PatioUseCase.Cartao::doca).isEqualTo("Doca 1");
        PatioUseCase.Cartao desc = q.colunas().get(PatioUseCase.Coluna.DESCARREGANDO).get(0);
        assertThat(desc.transportadora()).isEqualTo("Transportes Sul");
        assertThat(desc.modal()).isEqualTo("Granel");
        assertThat(desc.taxaDescarga()).isEqualByComparingTo("600.00");
        PatioUseCase.Cartao lib = q.colunas().get(PatioUseCase.Coluna.LIBERADO).get(0);
        assertThat(lib.paletes()).isEqualTo(8);
        assertThat(lib.taxaDescarga()).as("baú deduzido do caminhão").isEqualByComparingTo("240.00");
        assertThat(q.docas()).hasSize(2).allMatch(PatioUseCase.SituacaoDoca::ocupada);
        assertThat(q.indicador().lotadoAgora()).isTrue();
        assertThat(q.indicador().caminhoesNoPatio()).isEqualTo(3);
        assertThat(q.indicador().diaCheio()).isTrue();

        patio.atribuirDoca(3L, null, ADMIN);
        PatioUseCase.Quadro depois = patio.quadro(SEGUNDA, 1L);
        assertThat(depois.indicador().docasOcupadas()).isEqualTo(1);
        assertThat(depois.indicador().lotadoAgora()).isFalse();
        assertThat(depois.docas().get(0).placa()).isNull();
    }

    @Test
    void logisticaRegistraLojaETransportadoraEDevolveAlertasDaLoja() {
        agendamento(8L, StatusAgendamento.AGENDADO, 1L);
        capacidade.salvar(null, new CapacidadeRecebimentoUseCase.Dados(null, "Loja 7", null, 10, null), ADMIN);
        logistica.consumo = new LogisticaRepositorio.Consumo(12, 1);
        List<String> alertas = patio.registrarLogistica(8L, "Loja 7", "Rápido Sul", "bau", ADMIN);
        assertThat(alertas).singleElement().asString().contains("Loja 7");
        LogisticaAgendamento l = patio.logistica(8L);
        assertThat(l.entregaNaLoja()).isTrue();
        assertThat(l.modalDescarga()).isEqualTo("BAU");
        assertThat(patio.registrarLogistica(8L, null, null, " ", ADMIN)).isEmpty();
        assertThat(patio.logistica(8L).entregaNaLoja()).isFalse();
        assertThat(patio.logistica(99L)).isEqualTo(LogisticaAgendamento.vazia(99L));
        assertThatThrownBy(() -> patio.registrarLogistica(99L, null, null, null, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---- Produto avulso e catálogo
    @Test
    void produtoAvulsoUsaCatalogoOuCadastraNovo() {
        ProdutoRepositorio produtos = mock(ProdutoRepositorio.class);
        ItemAvulsoRepositorio itens = mock(ItemAvulsoRepositorio.class);
        when(itens.salvar(any())).thenAnswer(i -> i.getArgument(0));
        Produto arroz = new Produto(10L, "ARZ5", "7891234567895", "Arroz 5kg", "FD", new BigDecimal("5.1"), 50, true);
        when(produtos.buscarPorId(10L)).thenReturn(Optional.of(arroz));
        when(produtos.buscarPorCodigo(any())).thenReturn(Optional.empty());
        when(produtos.buscarPorEan("7891234567895")).thenReturn(Optional.of(arroz));
        when(produtos.salvar(any())).thenAnswer(i -> {
            Produto p = i.getArgument(0);
            return new Produto(11L, p.codigo(), p.ean(), p.descricao(), p.unidade(), null, null, true);
        });
        ItemAvulsoUseCase uc = new ItemAvulsoUseCase(itens, produtos, agendamentos, auditoria, TX, CLOCK);
        agendamento(1L, StatusAgendamento.AGENDADO, 1L);
        agendamento(2L, StatusAgendamento.EM_RECEBIMENTO, 1L);

        assertThatThrownBy(() -> uc.adicionar(1L, new ItemAvulsoUseCase.Dados(null, "X1", "Item", BigDecimal.ONE, null, null, false), ADMIN))
                .hasMessageContaining("chegada");
        ItemAvulso porId = uc.adicionar(2L, new ItemAvulsoUseCase.Dados(10L, null, null, new BigDecimal("3"), null, "sobra", false), ADMIN);
        assertThat(porId.descricao()).isEqualTo("Arroz 5kg");
        assertThat(porId.unidade()).isEqualTo("FD");
        assertThat(uc.adicionar(2L, new ItemAvulsoUseCase.Dados(null, "7891234567895", "x", BigDecimal.ONE, "UN", null, false), ADMIN).produtoId())
                .isEqualTo(10L);
        ItemAvulso novo = uc.adicionar(2L, new ItemAvulsoUseCase.Dados(null, "BRINDE1", "Brinde display", BigDecimal.ONE, "UN", null, true), ADMIN);
        assertThat(novo.produtoId()).isEqualTo(11L);
        ItemAvulso solto = uc.adicionar(2L, new ItemAvulsoUseCase.Dados(null, "AVULSO", "Sem cadastro", BigDecimal.TEN, null, null, false), ADMIN);
        assertThat(solto.produtoId()).isNull();
        assertThat(solto.unidade()).isNull();
        assertThatThrownBy(() -> uc.adicionar(2L, new ItemAvulsoUseCase.Dados(99L, null, null, BigDecimal.ONE, null, null, false), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> uc.adicionar(2L, new ItemAvulsoUseCase.Dados(null, " ", "x", BigDecimal.ONE, null, null, false), ADMIN))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new ItemAvulso(null, 1L, null, "A", "B", BigDecimal.ZERO, null, null, "u", null)).hasMessageContaining("Quantidade");
        assertThatThrownBy(() -> new ItemAvulso(null, null, null, "A", "B", BigDecimal.ONE, null, null, "u", null)).hasMessageContaining("Agendamento");
        assertThatThrownBy(() -> uc.adicionar(55L, new ItemAvulsoUseCase.Dados(null, "A", "B", BigDecimal.ONE, null, null, false), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        when(itens.listar(2L)).thenReturn(List.of(solto));
        assertThat(uc.listar(2L)).hasSize(1);

        when(produtos.listar(null, null)).thenReturn(List.of(arroz, new Produto(12L, "=CMD", null, "Item; \"especial\"", null, null, null, false)));
        String csv = uc.exportarCatalogoCsv(null);
        assertThat(csv).startsWith("\uFEFFcodigo;ean;descricao");
        assertThat(csv).contains("ARZ5;7891234567895;Arroz 5kg;FD;5,1;50;ativo");
        assertThat(csv).contains("'=CMD;;\"Item; \"\"especial\"\"\";;;;inativo");
        assertThat(CatalogoProdutoCsv.gerar(List.of())).hasLineCount(1);
    }

    // ---- Vale-palete: exigir as duas assinaturas, agrupamento e tipos por CD
    @Test
    void configuracaoExigeDuasAssinaturasAntesDaDevolucao() {
        Map<String, String> mem = new HashMap<>();
        ConfiguracaoRepositorio repo = new ConfiguracaoRepositorio() {
            public Optional<String> obter(String c) { return Optional.ofNullable(mem.get(c)); }
            public void salvar(String c, String v) { mem.put(c, v); }
        };
        ConfiguracaoValePaleteUseCase config = new ConfiguracaoValePaleteUseCase(repo, auditoria, TX);
        assertThat(config.obter()).isEqualTo(new ConfiguracaoValePaleteUseCase.Config(false, "fornecedor"));
        assertThat(config.salvar(new ConfiguracaoValePaleteUseCase.Config(true, "transportadora"), ADMIN).agrupamento()).isEqualTo("transportadora");
        assertThatThrownBy(() -> config.salvar(new ConfiguracaoValePaleteUseCase.Config(true, "loja"), ADMIN)).hasMessageContaining("Agrupamento");
        assertThatThrownBy(() -> config.salvar(new ConfiguracaoValePaleteUseCase.Config(true, null), ADMIN)).hasMessageContaining("Agrupamento");
        assertThatThrownBy(() -> config.salvar(null, ADMIN)).hasMessageContaining("Agrupamento");

        ValePaleteRepositorio vales = mock(ValePaleteRepositorio.class);
        ValePalete vale = new ValePalete(1L, "VP-20261102-0001", null, SEGUNDA, "Fornecedor", null, null,
                ValePalete.Quantidades.de("PBR", 10), ValePalete.Quantidades.ZERO, ValePalete.Status.PENDENTE, null, null, null,
                "admin", OffsetDateTime.now(CLOCK));
        when(vales.buscarPorId(1L)).thenReturn(Optional.of(vale));
        when(vales.salvar(any())).thenAnswer(i -> i.getArgument(0));
        ValePaleteUseCase uc = new ValePaleteUseCase(vales, agendamentos, auditoria, TX, CLOCK).comConfiguracao(config);
        assertThatThrownBy(() -> uc.registrarDevolucao(1L, ValePalete.Quantidades.de("PBR", 2), ADMIN)).hasMessageContaining("duas assinaturas");
        String png = "data:image/png;base64,AAAA";
        uc.assinar(1L, ValePalete.Assinante.MOTORISTA, "Motorista", png, ADMIN);
        assertThatThrownBy(() -> uc.registrarDevolucao(1L, ValePalete.Quantidades.de("PBR", 2), ADMIN)).hasMessageContaining("duas assinaturas");
        uc.assinar(1L, ValePalete.Assinante.CONFERENTE, "Conferente", png, ADMIN);
        assertThat(uc.registrarDevolucao(1L, ValePalete.Quantidades.de("PBR", 2), ADMIN).saldo().total()).isEqualTo(8);

        config.salvar(new ConfiguracaoValePaleteUseCase.Config(false, "fornecedor"), ADMIN);
        uc.refazerAssinatura(1L, ValePalete.Assinante.MOTORISTA, ADMIN);
        assertThat(uc.registrarDevolucao(1L, ValePalete.Quantidades.de("PBR", 1), ADMIN).saldo().total()).isEqualTo(7);
    }

    @Test
    void tipoDePaletePorCd() {
        TipoPalete todos = new TipoPalete(1L, "PBR", "PBR", BigDecimal.ONE, 1, true);
        TipoPalete soNorte = new TipoPalete(2L, "CAIXA_NORTE", "Caixa", BigDecimal.ONE, 2, true, 2L);
        assertThat(todos.valeParaCd(2L)).isTrue();
        assertThat(soNorte.valeParaCd(2L)).isTrue();
        assertThat(soNorte.valeParaCd(1L)).isFalse();
        assertThat(soNorte.valeParaCd(null)).isTrue();
        PaletesConfiguraveisNotificacoesTest.TiposMem repo = new PaletesConfiguraveisNotificacoesTest.TiposMem();
        repo.salvar(todos);
        repo.salvar(soNorte);
        var uc = new br.com.redekrill.agendamento.application.usecase.TipoPaleteUseCase(repo, auditoria, TX);
        assertThat(uc.listar(true, 1L)).extracting(TipoPalete::codigo).containsExactly("PBR");
        assertThat(uc.listar(true, 2L)).hasSize(2);
        TipoPalete criado = uc.criar(new br.com.redekrill.agendamento.application.usecase.TipoPaleteUseCase.Dados(
                "LOCAL", "Local", BigDecimal.ONE, 3, true, 2L), ADMIN);
        assertThat(criado.cdId()).isEqualTo(2L);
    }
}
