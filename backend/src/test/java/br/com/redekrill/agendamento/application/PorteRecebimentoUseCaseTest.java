package br.com.redekrill.agendamento.application;

import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.application.dto.NovoAgendamento;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.AuditoriaRepositorio;
import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.application.port.CobrancaPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.FaixaHorariaRepositorio;
import br.com.redekrill.agendamento.application.port.FornecedorRepositorio;
import br.com.redekrill.agendamento.application.port.GeradorToken;
import br.com.redekrill.agendamento.application.port.MovimentoPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.NotaFiscalRepositorio;
import br.com.redekrill.agendamento.application.port.NotificadorEmail;
import br.com.redekrill.agendamento.application.port.NotificadorPush;
import br.com.redekrill.agendamento.application.port.ParametroDescargaRepositorio;
import br.com.redekrill.agendamento.application.port.ParametroRepositorio;
import br.com.redekrill.agendamento.application.port.ProdutoRepositorio;
import br.com.redekrill.agendamento.application.port.TipoCaminhaoRepositorio;
import br.com.redekrill.agendamento.application.port.TipoPaletizacaoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.application.port.ValePaleteRepositorio;
import br.com.redekrill.agendamento.application.usecase.AgendamentoUseCase;
import br.com.redekrill.agendamento.application.usecase.AuditoriaUseCase;
import br.com.redekrill.agendamento.application.usecase.CadastroRecebimentoUseCase;
import br.com.redekrill.agendamento.application.usecase.FornecedorLinkUseCase;
import br.com.redekrill.agendamento.application.usecase.NotaFiscalUseCase;
import br.com.redekrill.agendamento.application.usecase.PaleteFinanceiroUseCase;
import br.com.redekrill.agendamento.application.usecase.RelatorioRecebimentoUseCase;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.CentroDistribuicao;
import br.com.redekrill.agendamento.domain.model.CobrancaPalete;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.Fornecedor;
import br.com.redekrill.agendamento.domain.model.MovimentoPalete;
import br.com.redekrill.agendamento.domain.model.NotaFiscal;
import br.com.redekrill.agendamento.domain.model.ParametroCapacidade;
import br.com.redekrill.agendamento.domain.model.ParametroDescarga;
import br.com.redekrill.agendamento.domain.model.Produto;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import br.com.redekrill.agendamento.domain.model.TipoCaminhao;
import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Casos de uso do porte completo do recebimento, com repositórios em memória. */
class PorteRecebimentoUseCaseTest {

    static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    /** 02/11/2026 11:00 em São Paulo. */
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T14:00:00Z"), SP);
    static final LocalDate HOJE = LocalDate.of(2026, 11, 2);
    static final OffsetDateTime AGORA = OffsetDateTime.now(CLOCK);
    static final Solicitante ADMIN = new Solicitante("admin", "10.0.0.1");
    static final FaixaHoraria F1 = new FaixaHoraria(1L, LocalTime.of(8, 0), LocalTime.of(10, 0), 120, true);
    static final FaixaHoraria F2 = new FaixaHoraria(2L, LocalTime.of(10, 0), LocalTime.of(12, 0), 120, true);
    static final FaixaHoraria F3 = new FaixaHoraria(3L, LocalTime.of(13, 0), LocalTime.of(15, 0), 120, true);
    static final TipoCaminhao TRUCK = new TipoCaminhao(1L, "Truck", null, 14, true);
    static final TipoPaletizacao PAL = new TipoPaletizacao(1L, "Paletizado", 1, true);
    static final Transacao TX = new Transacao() {
        @Override
        public <T> T executar(Supplier<T> op) {
            return op.get();
        }
    };
    static final GeradorToken TOKENS = new GeradorToken() {
        @Override
        public String novo() {
            return "token-fixo-para-os-testes-0001";
        }

        @Override
        public String hash(String t) {
            return "h:" + t;
        }
    };

    // ---------- fakes
    final Map<Long, Agendamento> ags = new LinkedHashMap<>();
    final AtomicLong seq = new AtomicLong(100);
    final AgendamentoRepositorio agRepo = new AgendamentoRepositorio() {
        public Agendamento salvar(Agendamento a) {
            Agendamento s = a.getId() == null ? copia(a, seq.incrementAndGet()) : a;
            ags.put(s.getId(), s);
            return s;
        }

        public Optional<Agendamento> buscarPorId(Long id) {
            return Optional.ofNullable(ags.get(id));
        }

        public List<Agendamento> listarPorPeriodo(LocalDate i, LocalDate f) {
            return ags.values().stream().filter(a -> !a.getData().isBefore(i) && !a.getData().isAfter(f)).toList();
        }

        public List<Agendamento> listar(FiltroAgendamento f) {
            return ags.values().stream()
                    .filter(a -> f.inicio() == null || !a.getData().isBefore(f.inicio()))
                    .filter(a -> f.fim() == null || !a.getData().isAfter(f.fim()))
                    .filter(a -> f.status() == null || a.getStatus() == f.status())
                    .filter(a -> f.cdId() == null || f.cdId().equals(a.getCdId()))
                    .filter(a -> !f.divergentes() || a.getDivergencia() != null)
                    .filter(a -> f.fornecedor() == null || a.getFornecedor().toLowerCase().contains(f.fornecedor().toLowerCase()))
                    .toList();
        }

        public void bloquearDia(LocalDate d) {
        }

        public Optional<Agendamento> buscarPorTokenHash(String h) {
            return ags.values().stream().filter(a -> h.equals(a.getTokenFornecedorHash())).findFirst();
        }
    };

    static Agendamento copia(Agendamento a, Long id) {
        return Agendamento.builder().id(id).data(a.getData()).faixaHorariaId(a.getFaixaHorariaId()).idPedido(a.getIdPedido())
                .fornecedor(a.getFornecedor()).emailFornecedor(a.getEmailFornecedor()).tipoCaminhaoId(a.getTipoCaminhaoId())
                .tipoPaletizacaoId(a.getTipoPaletizacaoId()).quantidadePaletes(a.getQuantidadePaletes())
                .paletesEquivalentes(a.getPaletesEquivalentes()).status(a.getStatus()).criadoPor(a.getCriadoPor())
                .criadoEm(a.getCriadoEm()).tokenFornecedorHash(a.getTokenFornecedorHash()).cdId(a.getCdId())
                .fornecedorId(a.getFornecedorId()).build();
    }

    static class Mem<T> {
        final Map<Long, T> m = new LinkedHashMap<>();
        final AtomicLong s = new AtomicLong();

        Collection<T> todos() {
            return m.values();
        }
    }

    final Mem<CentroDistribuicao> cdMem = new Mem<>();
    final CentroDistribuicaoRepositorio cds = new CentroDistribuicaoRepositorio() {
        public List<CentroDistribuicao> listar() {
            return List.copyOf(cdMem.todos());
        }

        public Optional<CentroDistribuicao> buscarPorId(Long id) {
            return Optional.ofNullable(cdMem.m.get(id));
        }

        public boolean existeCodigo(String c, Long ignorar) {
            return cdMem.todos().stream().anyMatch(x -> x.codigo().equalsIgnoreCase(c) && !Objects.equals(x.id(), ignorar));
        }

        public CentroDistribuicao salvar(CentroDistribuicao c) {
            long id = c.id() == null ? cdMem.s.incrementAndGet() : c.id();
            CentroDistribuicao n = new CentroDistribuicao(id, c.codigo(), c.nome(), c.cidade(), c.capacidadeDiariaPaletes(),
                    c.capacidadeFaixaPaletes(), c.ativo());
            cdMem.m.put(id, n);
            return n;
        }
    };

    final Mem<Fornecedor> fMem = new Mem<>();
    final FornecedorRepositorio fornecedores = new FornecedorRepositorio() {
        public List<Fornecedor> listar(String b, Boolean ativo) {
            return fMem.todos().stream().filter(f -> ativo == null || f.ativo() == ativo).toList();
        }

        public Optional<Fornecedor> buscarPorId(Long id) {
            return Optional.ofNullable(fMem.m.get(id));
        }

        public Optional<Fornecedor> buscarPorCnpj(String c) {
            return fMem.todos().stream().filter(f -> f.cnpj().equals(c)).findFirst();
        }

        public Fornecedor salvar(Fornecedor f) {
            long id = f.id() == null ? fMem.s.incrementAndGet() : f.id();
            Fornecedor n = new Fornecedor(id, f.cnpj(), f.razaoSocial(), f.nomeFantasia(), f.email(), f.telefone(), f.contato(), f.ativo());
            fMem.m.put(id, n);
            return n;
        }
    };

    final Mem<Produto> pMem = new Mem<>();
    final ProdutoRepositorio produtos = new ProdutoRepositorio() {
        public List<Produto> listar(String b, Boolean semPeso) {
            return pMem.todos().stream().filter(p -> !Boolean.TRUE.equals(semPeso) || p.pesoBrutoKg() == null).toList();
        }

        public Optional<Produto> buscarPorId(Long id) {
            return Optional.ofNullable(pMem.m.get(id));
        }

        public Optional<Produto> buscarPorCodigo(String c) {
            return pMem.todos().stream().filter(p -> p.codigo().equals(c)).findFirst();
        }

        public Optional<Produto> buscarPorEan(String e) {
            return pMem.todos().stream().filter(p -> e.equals(p.ean())).findFirst();
        }

        public Produto salvar(Produto p) {
            long id = p.id() == null ? pMem.s.incrementAndGet() : p.id();
            Produto n = new Produto(id, p.codigo(), p.ean(), p.descricao(), p.unidade(), p.pesoBrutoKg(), p.itensPorPalete(), p.ativo());
            pMem.m.put(id, n);
            return n;
        }
    };

    final Mem<NotaFiscal> nMem = new Mem<>();
    final NotaFiscalRepositorio notas = new NotaFiscalRepositorio() {
        public NotaFiscal salvar(NotaFiscal n) {
            long id = nMem.s.incrementAndGet();
            NotaFiscal x = new NotaFiscal(id, n.agendamentoId(), n.chave(), n.numero(), n.serie(), n.emitenteCnpj(), n.emitenteNome(),
                    n.emissao(), n.valorTotal(), n.pesoBrutoKg(), n.volumes(), n.itens(), n.importadoPor(), n.importadoEm());
            nMem.m.put(id, x);
            return x;
        }

        public Optional<NotaFiscal> buscarPorId(Long id) {
            return Optional.ofNullable(nMem.m.get(id));
        }

        public boolean existeChave(String c) {
            return nMem.todos().stream().anyMatch(n -> n.chave().equals(c));
        }

        public List<NotaFiscal> listarPorAgendamento(Long id) {
            return nMem.todos().stream().filter(n -> n.agendamentoId().equals(id)).toList();
        }

        public List<NotaFiscal> listarPorAgendamentos(List<Long> ids) {
            return nMem.todos().stream().filter(n -> ids.contains(n.agendamentoId())).toList();
        }

        public void excluir(Long id) {
            nMem.m.remove(id);
        }
    };

    final Map<Long, ParametroDescarga> descargaMem = new LinkedHashMap<>();
    final ParametroDescargaRepositorio descarga = new ParametroDescargaRepositorio() {
        public List<ParametroDescarga> listar() {
            return List.copyOf(descargaMem.values());
        }

        public ParametroDescarga salvar(ParametroDescarga p) {
            descargaMem.put(p.tipoCaminhaoId(), p);
            return p;
        }
    };

    final List<ValePalete> valesMem = new ArrayList<>();
    final ValePaleteRepositorio vales = new ValePaleteRepositorio() {
        public ValePalete salvar(ValePalete v) {
            valesMem.add(v);
            return v;
        }

        public Optional<ValePalete> buscarPorId(Long id) {
            return Optional.empty();
        }

        public Optional<ValePalete> buscarPorCodigo(String c) {
            return Optional.empty();
        }

        public List<ValePalete> listar(LocalDate i, LocalDate f, ValePalete.Status s, String forn) {
            return valesMem.stream().filter(v -> s == null || v.getStatus() == s)
                    .filter(v -> forn == null || v.getFornecedor().toLowerCase().contains(forn.toLowerCase())).toList();
        }

        public long proximaSequencia() {
            return valesMem.size() + 1;
        }
    };

    final Mem<CobrancaPalete> cMem = new Mem<>();
    final CobrancaPaleteRepositorio cobrancas = new CobrancaPaleteRepositorio() {
        public CobrancaPalete salvar(CobrancaPalete c) {
            long id = c.getId() == null ? cMem.s.incrementAndGet() : c.getId();
            CobrancaPalete n = new CobrancaPalete(id, c.getFornecedor(), c.getQuantidade(), c.getValorUnitario(), c.getVencimento(),
                    c.getObservacao(), c.getCriadoPor(), c.getCriadoEm(), c.getStatus(), c.getReciboNumero(), c.getFormaPagamento(),
                    c.getPagoEm(), c.getRecebidoPor(), c.getMotivoCancelamento());
            cMem.m.put(id, n);
            return n;
        }

        public Optional<CobrancaPalete> buscarPorId(Long id) {
            return Optional.ofNullable(cMem.m.get(id));
        }

        public List<CobrancaPalete> listar(String f, CobrancaPalete.Status s) {
            return cMem.todos().stream().filter(c -> s == null || c.getStatus() == s)
                    .filter(c -> f == null || c.getFornecedor().toLowerCase().contains(f.toLowerCase())).toList();
        }

        public long proximoRecibo() {
            return 7;
        }
    };

    final List<MovimentoPalete> movMem = new ArrayList<>();
    final MovimentoPaleteRepositorio movimentos = new MovimentoPaleteRepositorio() {
        public MovimentoPalete salvar(MovimentoPalete m) {
            MovimentoPalete n = new MovimentoPalete((long) movMem.size() + 1, m.data(), m.tipo(), m.cdId(), m.loja(), m.romaneio(),
                    m.quantidades(), m.observacao(), m.criadoPor(), m.criadoEm());
            movMem.add(n);
            return n;
        }

        public List<MovimentoPalete> listar(String l, LocalDate i, LocalDate f) {
            return movMem.stream().filter(m -> l == null || m.loja().toLowerCase().contains(l.toLowerCase())).toList();
        }

        public long proximoRomaneio() {
            return movMem.size() + 1;
        }
    };

    final NotificadorPush push = mock(NotificadorPush.class);
    final AuditoriaUseCase auditoria = new AuditoriaUseCase(mock(AuditoriaRepositorio.class), CLOCK);
    AgendamentoUseCase agendamentos;
    CadastroRecebimentoUseCase cadastros;
    NotaFiscalUseCase nfe;
    RelatorioRecebimentoUseCase relatorios;
    PaleteFinanceiroUseCase paletes;
    FornecedorLinkUseCase link;
    TipoCaminhaoRepositorio caminhoes;

    @BeforeEach
    void setUp() {
        FaixaHorariaRepositorio faixas = mock(FaixaHorariaRepositorio.class);
        org.mockito.Mockito.when(faixas.listar()).thenReturn(List.of(F1, F2, F3));
        org.mockito.Mockito.when(faixas.buscarPorId(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(i -> List.of(F1, F2, F3).stream().filter(f -> f.id().equals(i.getArgument(0))).findFirst());
        caminhoes = mock(TipoCaminhaoRepositorio.class);
        org.mockito.Mockito.when(caminhoes.listar()).thenReturn(List.of(TRUCK));
        org.mockito.Mockito.when(caminhoes.buscarPorId(1L)).thenReturn(Optional.of(TRUCK));
        TipoPaletizacaoRepositorio pal = mock(TipoPaletizacaoRepositorio.class);
        org.mockito.Mockito.when(pal.listar()).thenReturn(List.of(PAL));
        org.mockito.Mockito.when(pal.buscarPorId(1L)).thenReturn(Optional.of(PAL));
        ParametroRepositorio param = mock(ParametroRepositorio.class);
        org.mockito.Mockito.when(param.obter()).thenReturn(ParametroCapacidade.PADRAO);
        cds.salvar(new CentroDistribuicao(null, "806", "CD Rede Krill", null, null, null, true));
        agendamentos = new AgendamentoUseCase(agRepo, faixas, caminhoes, pal, param, mock(NotificadorEmail.class), push,
                auditoria, TX, CLOCK, TOKENS, "http://app", cds, fornecedores);
        cadastros = new CadastroRecebimentoUseCase(cds, fornecedores, produtos, descarga, caminhoes, auditoria, TX);
        nfe = new NotaFiscalUseCase(notas, agRepo, produtos, fornecedores, auditoria, TX, CLOCK);
        relatorios = new RelatorioRecebimentoUseCase(agendamentos, faixas, descarga, notas, vales, cobrancas, CLOCK);
        paletes = new PaleteFinanceiroUseCase(vales, cobrancas, movimentos, cds, auditoria, TX, CLOCK);
        link = new FornecedorLinkUseCase(agRepo, agendamentos, TOKENS, auditoria, TX, CLOCK);
    }

    Agendamento ag(long id, LocalDate data, long faixa, int paletes, StatusAgendamento st) {
        Agendamento a = Agendamento.builder().id(id).data(data).faixaHorariaId(faixa).idPedido(id).fornecedor("Fornecedor " + id)
                .emailFornecedor("f" + id + "@x.com").tipoCaminhaoId(1L).tipoPaletizacaoId(1L).quantidadePaletes(paletes)
                .paletesEquivalentes(paletes).status(st).build();
        ags.put(id, a);
        return a;
    }

    NovoAgendamento novo(Long cd, Long fornecedor, int paletes) {
        return new NovoAgendamento(HOJE.plusDays(1), 1L, 1L, "Fornecedor", "f@x.com", 1L, 1L, paletes, null, cd, fornecedor);
    }

    static byte[] xml() throws Exception {
        try (var in = PorteRecebimentoUseCaseTest.class.getResourceAsStream("/nfe/nfe-exemplo.xml")) {
            return in.readAllBytes();
        }
    }

    @Test
    void multiCdUsaCapacidadePropriaESeparaOcupacao() {
        CentroDistribuicao cd2 = cadastros.salvarCd(new CentroDistribuicao(null, "807", "CD 2", "Itu", 100, 30, true), ADMIN);
        assertThat(cadastros.listarCds()).hasSize(2);
        var r = agendamentos.criar(novo(cd2.id(), null, 40), ADMIN);
        assertThat(r.agendamento().agendamento().getCdId()).isEqualTo(cd2.id());
        assertThat(r.alertas()).extracting(a -> a.tipo().name()).containsExactly("FAIXA");
        var ocupCd2 = agendamentos.ocupacaoDoDia(HOJE.plusDays(1), cd2.id());
        assertThat(ocupCd2.capacidade()).isEqualTo(100);
        assertThat(ocupCd2.paletesOcupados()).isEqualTo(40);
        assertThat(ocupCd2.faixas().get(0).capacidade()).isEqualTo(30);
        assertThat(agendamentos.ocupacaoDoDia(HOJE.plusDays(1)).paletesOcupados()).isZero();
        assertThat(agendamentos.ocupacaoPeriodo(HOJE, HOJE.plusDays(1), cd2.id())).hasSize(2);
        assertThat(agendamentos.simular(HOJE.plusDays(1), 1L, 1L, 1, cd2.id()).alertas()).isNotEmpty();
        assertThatThrownBy(() -> agendamentos.ocupacaoDoDia(HOJE, 99L)).isInstanceOf(RecursoNaoEncontradoException.class);
        cadastros.salvarCd(new CentroDistribuicao(cd2.id(), "807", "CD 2", "Itu", 100, 30, false), ADMIN);
        assertThatThrownBy(() -> agendamentos.criar(novo(cd2.id(), null, 1), ADMIN)).hasMessage("CD inativo");
        assertThatThrownBy(() -> cadastros.salvarCd(new CentroDistribuicao(null, "806", "Dup", null, null, null, true), ADMIN))
                .hasMessageContaining("código");
        assertThatThrownBy(() -> cadastros.salvarCd(new CentroDistribuicao(1L, "806", "CD", null, null, null, false), ADMIN))
                .hasMessageContaining("padrão");
        assertThatThrownBy(() -> cadastros.salvarCd(new CentroDistribuicao(50L, "X", "Y", null, null, null, true), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void fornecedorCadastradoNoAgendamento() {
        Fornecedor f = cadastros.salvarFornecedor(new Fornecedor(null, "11222333000181", "Litoral", null, "a@b.com", null, null, true), ADMIN);
        assertThat(cadastros.fornecedor(f.id()).razaoSocial()).isEqualTo("Litoral");
        assertThat(cadastros.listarFornecedores(null, true)).hasSize(1);
        assertThatThrownBy(() -> cadastros.salvarFornecedor(new Fornecedor(null, "11222333000181", "Outro", null, "c@d.com", null, null, true), ADMIN))
                .hasMessageContaining("CNPJ já cadastrado");
        assertThat(agendamentos.criar(novo(null, f.id(), 10), ADMIN).agendamento().agendamento().getFornecedorId()).isEqualTo(f.id());
        cadastros.salvarFornecedor(new Fornecedor(f.id(), "11222333000181", "Litoral", null, "a@b.com", null, null, false), ADMIN);
        assertThatThrownBy(() -> agendamentos.criar(novo(null, f.id(), 10), ADMIN)).hasMessage("Fornecedor inativo");
        assertThatThrownBy(() -> agendamentos.criar(novo(null, 77L, 10), ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> cadastros.fornecedor(77L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void fornecedorRemarcaPeloLinkSoComVaga() {
        ag(1, HOJE.plusDays(2), 2, 110, StatusAgendamento.AGENDADO);
        Agendamento a = ag(2, HOJE.plusDays(1), 1, 20, StatusAgendamento.CONFIRMADO);
        a.definirTokenFornecedor("h:token-fixo-para-os-testes-0001");
        assertThat(link.disponibilidade("token-fixo-para-os-testes-0001")).hasSize(14).first()
                .satisfies(d -> assertThat(d.data()).isEqualTo(HOJE));
        assertThatThrownBy(() -> link.reagendar("token-fixo-para-os-testes-0001", HOJE.plusDays(2), 2L, "1.1.1.1"))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("não tem vaga");
        var d = link.reagendar("token-fixo-para-os-testes-0001", HOJE.plusDays(2), 3L, "1.1.1.1");
        assertThat(d.agendamento().getData()).isEqualTo(HOJE.plusDays(2));
        assertThat(d.agendamento().getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        verify(push).notificarTodos(eq("Agendamento remarcado"), anyString(), anyString());
        // o CD pode exceder (só alerta)
        var r = agendamentos.reagendar(2L, HOJE.plusDays(2), 2L, ADMIN, true);
        assertThat(r.alertas()).isNotEmpty();
        verify(push).notificarTodos(eq("Capacidade excedida"), anyString(), anyString());
        assertThatThrownBy(() -> agendamentos.reagendar(2L, null, 2L, ADMIN, true)).hasMessage("Data é obrigatória");
        assertThatThrownBy(() -> agendamentos.reagendar(999L, HOJE, 2L, ADMIN, true)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void importaNfeCriaProdutosCalculaPesoEVinculaFornecedor() throws Exception {
        Fornecedor f = cadastros.salvarFornecedor(new Fornecedor(null, "11222333000181", "Litoral", null, "a@b.com", null, null, true), ADMIN);
        produtos.salvar(new Produto(null, "ARZ5", "7891000100103", "Arroz", "FD", new BigDecimal("5.2"), 40, true));
        Agendamento a = ag(5, HOJE, 1, 10, StatusAgendamento.EM_RECEBIMENTO);
        var r = nfe.importar(5L, xml(), ADMIN);
        assertThat(r.produtosCriados()).isEqualTo(1);
        assertThat(r.nota().itens()).hasSize(2);
        assertThat(r.nota().itens().get(0).pesoKg()).isEqualByComparingTo("520.000");
        assertThat(r.nota().itens().get(1).pesoKg()).isNull();
        assertThat(r.avisos()).anyMatch(s -> s.contains("FEJ1"));
        assertThat(a.getFornecedorId()).isEqualTo(f.id());
        assertThat(nfe.listar(5L)).hasSize(1);
        assertThatThrownBy(() -> nfe.importar(5L, xml(), ADMIN)).hasMessageContaining("já importada");
        assertThatThrownBy(() -> nfe.importar(999L, xml(), ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);

        // pesos cadastrados e reimportação: peso diverge do informado na NF (3600 kg)
        Produto feijao = cadastros.listarProdutos(null, true).get(0);
        cadastros.atualizarPeso(feijao.id(), new BigDecimal("1.02"), null, ADMIN);
        assertThat(cadastros.listarProdutos(null, true)).isEmpty();
        nfe.excluir(r.nota().id(), ADMIN);
        var r2 = nfe.importar(5L, xml(), ADMIN);
        assertThat(r2.nota().pesoCalculadoKg()).isEqualByComparingTo("571.000");
        assertThat(r2.avisos()).anyMatch(s -> s.contains("difere"));

        // emitente sem cadastro / diferente do agendamento
        Agendamento b = ag(6, HOJE, 1, 10, StatusAgendamento.CHEGOU);
        b.vincular(null, 999L);
        String outraChave = new String(xml()).replace("000123451000123458", "000123461000123459");
        assertThat(nfe.importar(6L, outraChave.getBytes(), ADMIN).avisos()).anyMatch(s -> s.contains("diferente"));
        fMem.m.clear();
        String terceira = new String(xml()).replace("000123451000123458", "000123471000123450");
        assertThat(nfe.importar(5L, terceira.getBytes(), ADMIN).avisos()).anyMatch(s -> s.contains("não está no cadastro"));

        Agendamento concluido = ags.get(5L);
        concluido.concluirRecebimento(10, null, AGORA);
        assertThatThrownBy(() -> nfe.excluir(r2.nota().id(), ADMIN)).hasMessageContaining("concluído");
        assertThatThrownBy(() -> nfe.excluir(999L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void cadastroDeProdutosEParametrosDeDescarga() {
        Produto p = cadastros.salvarProduto(new Produto(null, "P1", null, "Produto 1", "UN", null, null, true), ADMIN);
        assertThat(cadastros.salvarProduto(new Produto(p.id(), "P1", null, "Produto 1b", "UN", null, null, true), ADMIN).descricao())
                .isEqualTo("Produto 1b");
        assertThatThrownBy(() -> cadastros.salvarProduto(new Produto(null, "P1", null, "Dup", null, null, null, true), ADMIN))
                .hasMessageContaining("já cadastrado");
        assertThatThrownBy(() -> cadastros.salvarProduto(new Produto(50L, "P9", null, "X", null, null, null, true), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> cadastros.atualizarPeso(50L, BigDecimal.ONE, null, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
        cadastros.salvarParametroDescarga(new ParametroDescarga(1L, 45, 20, new BigDecimal("2.00")), ADMIN);
        assertThat(cadastros.listarParametrosDescarga()).hasSize(1);
        assertThatThrownBy(() -> cadastros.salvarParametroDescarga(new ParametroDescarga(9L, 45, 20, BigDecimal.ONE), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void divergenciasDescargaERotina() throws Exception {
        descarga.salvar(new ParametroDescarga(1L, 60, 30, new BigDecimal("3.50")));
        Agendamento rec = ag(10, HOJE, 1, 20, StatusAgendamento.AGENDADO);
        rec.registrarChegada("ABC1D23", null, AGORA.minusMinutes(150));
        rec.iniciarRecebimento("Carlos", AGORA.minusMinutes(120));
        rec.concluirRecebimento(18, null, AGORA.minusMinutes(30));
        Agendamento esperando = ag(11, HOJE, 2, 10, StatusAgendamento.AGENDADO);
        esperando.registrarChegada("ABC1D24", null, AGORA.minusMinutes(50));
        Agendamento descarregando = ag(12, HOJE, 2, 10, StatusAgendamento.AGENDADO);
        descarregando.registrarChegada("ABC1D25", null, AGORA.minusMinutes(100));
        descarregando.iniciarRecebimento("Ana", AGORA.minusMinutes(90));
        ag(13, HOJE, 1, 5, StatusAgendamento.AGENDADO);           // atrasado (faixa 08h)
        ag(14, HOJE.plusDays(1), 1, 5, StatusAgendamento.AGENDADO); // amanhã sem confirmação
        ag(15, HOJE, 3, 5, StatusAgendamento.AGENDADO);           // 13h: ainda não atrasou

        var div = relatorios.divergencias(HOJE, HOJE, null, null);
        assertThat(div).hasSize(1);
        assertThat(div.get(0).diferencaPaletes()).isEqualTo(-2);
        assertThat(div.get(0).notas()).isZero();

        var d = relatorios.descarga(HOJE, HOJE, null);
        assertThat(d.linhas()).hasSize(3);
        assertThat(d.resumo()).hasSize(1);
        assertThat(d.resumo().get(0).acimaDoPrevisto()).isEqualTo(1);
        assertThat(d.resumo().get(0).custoTotal()).isEqualByComparingTo("63.00");

        var rotina = relatorios.rotina(null, null);
        Map<String, Integer> n = new LinkedHashMap<>();
        rotina.itens().forEach(i -> n.put(i.codigo(), i.itens().size()));
        assertThat(rotina.data()).isEqualTo(HOJE);
        assertThat(n).containsEntry("SEM_CONFIRMACAO", 1).containsEntry("ATRASADOS", 1).containsEntry("ESPERA", 1)
                .containsEntry("DESCARGA_LONGA", 1).containsEntry("SEM_NFE", 1).containsEntry("DIVERGENCIAS", 1)
                .containsEntry("VALES", 0).containsEntry("COBRANCAS", 0);

        nfe.importar(10L, xml(), ADMIN);
        assertThat(relatorios.divergencias(HOJE, HOJE, "fornecedor", 1L).get(0).notas()).isEqualTo(1);
        assertThat(relatorios.rotina(HOJE.minusDays(1), null).itens().stream().filter(i -> i.codigo().equals("ATRASADOS"))
                .findFirst().orElseThrow().itens()).isEmpty();
        assertThatThrownBy(() -> relatorios.descarga(null, HOJE, null)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> relatorios.divergencias(HOJE, null, null, null)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void contaCorrenteCobrancaReciboEMovimentosDeLoja() {
        ValePalete v1 = new ValePalete(1L, "VP-1", null, HOJE, "Bebidas Vale Ltda", null, null,
                br.com.redekrill.agendamento.TestePaletes.q(20, 5, 0, 0), null, ValePalete.Status.PENDENTE, null, null, null, "admin", AGORA);
        v1.registrarDevolucao(br.com.redekrill.agendamento.TestePaletes.q(5, 0, 0, 0));
        vales.salvar(v1);
        ValePalete cancelado = new ValePalete(2L, "VP-2", null, HOJE, "Bebidas Vale Ltda", null, null,
                br.com.redekrill.agendamento.TestePaletes.q(9, 0, 0, 0), null, ValePalete.Status.PENDENTE, null, null, null, "admin", AGORA);
        cancelado.cancelar();
        vales.salvar(cancelado);
        assertThat(paletes.saldoEmAberto("bebidas vale ltda")).isEqualTo(20);

        var c = paletes.cobrar(new PaleteFinanceiroUseCase.NovaCobranca("Bebidas Vale Ltda", 15, new BigDecimal("40"), HOJE.plusDays(5), null), ADMIN);
        assertThat(c.valorTotal()).isEqualByComparingTo("600.00");
        assertThatThrownBy(() -> paletes.cobrar(new PaleteFinanceiroUseCase.NovaCobranca("Bebidas Vale Ltda", 10, BigDecimal.TEN,
                HOJE.plusDays(5), null), ADMIN)).isInstanceOf(RegraNegocioException.class);
        var s = paletes.saldoPorFornecedor(null).get(0);
        assertThat(s.entregues()).isEqualTo(25);
        assertThat(s.devolvidos()).isEqualTo(5);
        assertThat(s.cobrados()).isEqualTo(15);
        assertThat(s.saldoEmAberto()).isEqualTo(5);
        assertThat(s.valorEmAberto()).isEqualByComparingTo("600.00");

        var pago = paletes.pagar(c.getId(), CobrancaPalete.FormaPagamento.PIX, ADMIN);
        assertThat(pago.getReciboNumero()).isEqualTo("RP-20261102-0007");
        assertThat(paletes.saldoPorFornecedor("bebidas").get(0).pagos()).isEqualTo(15);
        var outra = paletes.cobrar(new PaleteFinanceiroUseCase.NovaCobranca("Bebidas Vale Ltda", 5, BigDecimal.ONE, HOJE, null), ADMIN);
        assertThat(paletes.cancelarCobranca(outra.getId(), "acordo comercial", ADMIN).getStatus()).isEqualTo(CobrancaPalete.Status.CANCELADA);
        assertThat(paletes.listarCobrancas(null, CobrancaPalete.Status.PAGA)).hasSize(1);
        assertThatThrownBy(() -> paletes.cobranca(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> paletes.cobrar(new PaleteFinanceiroUseCase.NovaCobranca("Bebidas Vale Ltda", null, BigDecimal.ONE, HOJE, null), ADMIN))
                .isInstanceOf(RegraNegocioException.class);

        var q = br.com.redekrill.agendamento.TestePaletes.q(30, 0, 0, 0);
        var envio = paletes.registrarMovimento(new PaleteFinanceiroUseCase.NovoMovimento(null, MovimentoPalete.Tipo.ENVIO, null,
                "Loja 12", q, null), ADMIN);
        assertThat(envio.romaneio()).isEqualTo("RM-20261102-0001");
        assertThat(envio.data()).isEqualTo(HOJE);
        paletes.registrarMovimento(new PaleteFinanceiroUseCase.NovoMovimento(HOJE.plusDays(1), MovimentoPalete.Tipo.RETORNO, 1L,
                "loja 12", br.com.redekrill.agendamento.TestePaletes.q(12, 0, 0, 0), "parcial"), ADMIN);
        paletes.registrarMovimento(new PaleteFinanceiroUseCase.NovoMovimento(HOJE.minusDays(3), MovimentoPalete.Tipo.SALDO_INICIAL, 1L,
                "Loja 3", br.com.redekrill.agendamento.TestePaletes.q(0, 4, 0, 0), null), ADMIN);
        var lojas = paletes.saldoPorLoja(null);
        assertThat(lojas).hasSize(2);
        var l12 = lojas.stream().filter(l -> l.loja().equals("Loja 12")).findFirst().orElseThrow();
        assertThat(l12.porTipo().get("PBR")).isEqualTo(18);
        assertThat(l12.ultimoMovimento()).isEqualTo(HOJE.plusDays(1));
        assertThat(paletes.listarMovimentos("loja 12", null, null)).hasSize(2);
        assertThatThrownBy(() -> paletes.registrarMovimento(new PaleteFinanceiroUseCase.NovoMovimento(null, MovimentoPalete.Tipo.ENVIO,
                9L, "X", q, null), ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);

        // cobrança vencida entra na rotina
        cobrancas.salvar(new CobrancaPalete(null, "Outro", 2, BigDecimal.ONE, HOJE.minusDays(2), null, "admin", AGORA,
                CobrancaPalete.Status.ABERTA, null, null, null, null, null));
        assertThat(relatorios.rotina(HOJE, null).itens().stream().filter(i -> i.codigo().equals("COBRANCAS"))
                .findFirst().orElseThrow().itens()).hasSize(1);
    }
}
