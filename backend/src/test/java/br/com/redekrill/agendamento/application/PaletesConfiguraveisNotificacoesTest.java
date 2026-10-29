package br.com.redekrill.agendamento.application;

import br.com.redekrill.agendamento.TestePaletes;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.AuditoriaRepositorio;
import br.com.redekrill.agendamento.application.port.CanalEmailDocumentos;
import br.com.redekrill.agendamento.application.port.CentroDistribuicaoRepositorio;
import br.com.redekrill.agendamento.application.port.CobrancaPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.ConferenteRepositorio;
import br.com.redekrill.agendamento.application.port.FornecedorRepositorio;
import br.com.redekrill.agendamento.application.port.MovimentoPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.NotificacaoEnvioRepositorio;
import br.com.redekrill.agendamento.application.port.NotificadorWhatsApp;
import br.com.redekrill.agendamento.application.port.TipoPaleteRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.application.port.ValePaleteRepositorio;
import br.com.redekrill.agendamento.application.usecase.AgendamentoUseCase;
import br.com.redekrill.agendamento.application.usecase.AuditoriaUseCase;
import br.com.redekrill.agendamento.application.usecase.ConferenteUseCase;
import br.com.redekrill.agendamento.application.usecase.NotificacaoUseCase;
import br.com.redekrill.agendamento.application.usecase.PaleteFinanceiroUseCase;
import br.com.redekrill.agendamento.application.usecase.TipoPaleteUseCase;
import br.com.redekrill.agendamento.application.usecase.ValePaleteUseCase;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.CentroDistribuicao;
import br.com.redekrill.agendamento.domain.model.CobrancaPalete;
import br.com.redekrill.agendamento.domain.model.Conferente;
import br.com.redekrill.agendamento.domain.model.Fornecedor;
import br.com.redekrill.agendamento.domain.model.MovimentoPalete;
import br.com.redekrill.agendamento.domain.model.NotificacaoEnvio;
import br.com.redekrill.agendamento.domain.model.TipoPalete;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import br.com.redekrill.agendamento.domain.service.WhatsAppLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tipos de palete configuráveis, conferentes por CD, e-mail de cobrança e canal WhatsApp. */
class PaletesConfiguraveisNotificacoesTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T14:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    static final Solicitante ADMIN = new Solicitante("admin", "10.0.0.1");
    static final Transacao TX = new Transacao() {
        @Override
        public <T> T executar(Supplier<T> op) {
            return op.get();
        }
    };

    /** Repositório de tipos em memória. */
    static class TiposMem implements TipoPaleteRepositorio {
        final List<TipoPalete> itens = new ArrayList<>();
        final java.util.Set<String> usados = new java.util.HashSet<>();

        public List<TipoPalete> listar() { return List.copyOf(itens); }
        public Optional<TipoPalete> buscarPorId(Long id) { return itens.stream().filter(t -> t.id().equals(id)).findFirst(); }
        public boolean existeCodigo(String c, Long ign) { return itens.stream().anyMatch(t -> t.codigo().equals(c) && !t.id().equals(ign)); }
        public boolean emUso(String c) { return usados.contains(c); }
        public TipoPalete salvar(TipoPalete t) {
            TipoPalete s = t.id() == null ? new TipoPalete((long) itens.size() + 1, t.codigo(), t.nome(), t.valorReposicao(), t.ordem(), t.ativo(), t.cdId()) : t;
            itens.removeIf(x -> x.id().equals(s.id()));
            itens.add(s);
            return s;
        }
        public void excluir(Long id) { itens.removeIf(t -> t.id().equals(id)); }
    }

    final AuditoriaUseCase auditoria = new AuditoriaUseCase(mock(AuditoriaRepositorio.class), CLOCK);
    TiposMem tiposRepo;
    TipoPaleteUseCase tipos;

    @BeforeEach
    void setUp() {
        tiposRepo = new TiposMem();
        tipos = new TipoPaleteUseCase(tiposRepo, auditoria, TX);
        for (String[] t : new String[][]{{"PBR", "PBR"}, {"CHEP", "CHEP"}, {"CHAPATEX", "Chapatex"}, {"DESCARTAVEL", "Descartável"}}) {
            tipos.criar(new TipoPaleteUseCase.Dados(t[0], t[1], BigDecimal.TEN, null, true), ADMIN);
        }
    }

    // ---- Tipos de palete
    @Test
    void tipoPaleteValidaCodigoNomeEValor() {
        assertThat(new TipoPalete(null, " pallet plastico ", "Plástico", null, 0, true).codigo()).isEqualTo("PALLET_PLASTICO");
        assertThatThrownBy(() -> new TipoPalete(null, "x", "X", null, 0, true)).hasMessageContaining("2 a 20");
        assertThatThrownBy(() -> new TipoPalete(null, "PBR-1", "X", null, 0, true)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new TipoPalete(null, "PBR", " ", null, 0, true)).hasMessageContaining("Nome");
        assertThatThrownBy(() -> new TipoPalete(null, "PBR", "PBR", new BigDecimal("-1"), 0, true)).hasMessageContaining("negativo");
    }

    @Test
    void crudDeTiposComCodigoUnicoImutavelEExclusaoSoSemUso() {
        TipoPalete novo = tipos.criar(new TipoPaleteUseCase.Dados("plastico", "Plástico", new BigDecimal("80"), 5, null), ADMIN);
        assertThat(novo.codigo()).isEqualTo("PLASTICO");
        assertThat(novo.ativo()).isTrue();
        assertThatThrownBy(() -> tipos.criar(new TipoPaleteUseCase.Dados("PBR", "Outro", null, null, true), ADMIN))
                .hasMessageContaining("Já existe");
        assertThatThrownBy(() -> tipos.atualizar(novo.id(), new TipoPaleteUseCase.Dados("OUTRO", "Plástico", null, null, true), ADMIN))
                .hasMessageContaining("não pode ser alterado");
        TipoPalete alterado = tipos.atualizar(novo.id(), new TipoPaleteUseCase.Dados(null, "Plástico azul", BigDecimal.ONE, null, false), ADMIN);
        assertThat(alterado.nome()).isEqualTo("Plástico azul");
        assertThat(alterado.ordem()).isEqualTo(5);
        assertThat(tipos.listar(true)).extracting(TipoPalete::codigo).doesNotContain("PLASTICO");

        tiposRepo.usados.add("PBR");
        assertThatThrownBy(() -> tipos.excluir(1L, ADMIN)).hasMessageContaining("Inative");
        tipos.excluir(novo.id(), ADMIN);
        assertThat(tipos.listar(false)).hasSize(4);
    }

    @Test
    void mantemAoMenosUmTipoAtivo() {
        for (long id = 2; id <= 4; id++) tipos.atualizar(id, new TipoPaleteUseCase.Dados(null, "T" + id, null, null, false), ADMIN);
        assertThatThrownBy(() -> tipos.atualizar(1L, new TipoPaleteUseCase.Dados(null, "PBR", null, null, false), ADMIN))
                .hasMessageContaining("ao menos um");
        assertThatThrownBy(() -> tipos.excluir(1L, ADMIN)).hasMessageContaining("ao menos um");
    }

    @Test
    void quantidadesAceitamQualquerTipoEValidamContraOCadastro() {
        var q = new ValePalete.Quantidades(Map.of("pbr", 3, "PLASTICO", 2, "CHEP", 0));
        assertThat(q.itens()).containsExactly(Map.entry("PBR", 3), Map.entry("PLASTICO", 2));
        assertThat(q.total()).isEqualTo(5);
        assertThat(q.menos(ValePalete.Quantidades.de("PBR", 5)).itens()).containsExactly(Map.entry("PLASTICO", 2));
        assertThatThrownBy(() -> tipos.validarNovo(q)).hasMessageContaining("PLASTICO");
        tipos.criar(new TipoPaleteUseCase.Dados("PLASTICO", "Plástico", null, null, true), ADMIN);
        tipos.validarNovo(q);
        tipos.atualizar(2L, new TipoPaleteUseCase.Dados(null, "CHEP", null, null, false), ADMIN);
        assertThatThrownBy(() -> tipos.validarNovo(TestePaletes.q(0, 1, 0, 0))).hasMessageContaining("inativo: CHEP");
    }

    @Test
    void valeComTipoNovoDevolveETipoInexistenteERecusado() {
        tipos.criar(new TipoPaleteUseCase.Dados("PLASTICO", "Plástico", null, null, true), ADMIN);
        ValePaleteRepositorio vales = mock(ValePaleteRepositorio.class);
        when(vales.salvar(any())).thenAnswer(i -> i.getArgument(0));
        ValePaleteUseCase uc = new ValePaleteUseCase(vales, mock(AgendamentoRepositorio.class), auditoria, TX, CLOCK, tipos);
        ValePalete v = uc.emitir(new ValePaleteUseCase.NovoVale(null, null, "Fornecedor X", null, null,
                new ValePalete.Quantidades(Map.of("PLASTICO", 4, "PBR", 1)), null), ADMIN);
        assertThat(v.getEntregues().de("PLASTICO")).isEqualTo(4);
        v.registrarDevolucao(ValePalete.Quantidades.de("PLASTICO", 4));
        assertThat(v.saldo().itens()).containsExactly(Map.entry("PBR", 1));
        assertThatThrownBy(() -> v.registrarDevolucao(ValePalete.Quantidades.de("CHEP", 1))).hasMessageContaining("saldo");
        assertThatThrownBy(() -> uc.emitir(new ValePaleteUseCase.NovoVale(null, null, "F", null, null,
                ValePalete.Quantidades.de("MADEIRA", 1), null), ADMIN)).hasMessageContaining("MADEIRA");
    }

    @Test
    void saldoDaLojaSomaPorTipoComSinalDoMovimento() {
        MovimentoPaleteRepositorio movs = mock(MovimentoPaleteRepositorio.class);
        OffsetDateTime agora = OffsetDateTime.now(CLOCK);
        when(movs.listar(any(), any(), any())).thenReturn(List.of(
                new MovimentoPalete(1L, LocalDate.of(2026, 11, 1), MovimentoPalete.Tipo.ENVIO, 1L, "Loja 1", "RM-1",
                        new ValePalete.Quantidades(Map.of("PBR", 10, "PLASTICO", 3)), null, "a", agora),
                new MovimentoPalete(2L, LocalDate.of(2026, 11, 2), MovimentoPalete.Tipo.RETORNO, 1L, "loja 1", "RM-2",
                        ValePalete.Quantidades.de("PLASTICO", 1), null, "a", agora)));
        var uc = new PaleteFinanceiroUseCase(mock(ValePaleteRepositorio.class), mock(CobrancaPaleteRepositorio.class), movs,
                mock(CentroDistribuicaoRepositorio.class), auditoria, TX, CLOCK, tipos);
        var s = uc.saldoPorLoja(null).get(0);
        assertThat(s.porTipo()).containsEntry("PBR", 10).containsEntry("PLASTICO", 2);
        assertThat(s.total()).isEqualTo(12);
        assertThat(s.ultimoMovimento()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThatThrownBy(() -> uc.registrarMovimento(new PaleteFinanceiroUseCase.NovoMovimento(null, MovimentoPalete.Tipo.ENVIO,
                1L, "Loja 1", ValePalete.Quantidades.de("XPTO", 1), null), ADMIN)).hasMessageContaining("XPTO");
    }

    // ---- Conferentes
    @Test
    void conferentePorCdExigeNomeCompletoUnicoEValidaInicioDaConferencia() {
        assertThatThrownBy(() -> new Conferente(null, 1L, "Carlos", true)).hasMessageContaining("nome completo");
        ConferenteRepositorio repo = mock(ConferenteRepositorio.class);
        CentroDistribuicaoRepositorio cds = mock(CentroDistribuicaoRepositorio.class);
        when(cds.buscarPorId(1L)).thenReturn(Optional.of(new CentroDistribuicao(1L, "CD1", "CD 1", null, null, null, true)));
        when(repo.existeNome(1L, "Ana Souza", null)).thenReturn(true);
        ConferenteUseCase uc = new ConferenteUseCase(repo, cds, auditoria, TX);
        assertThatThrownBy(() -> uc.salvar(null, new ConferenteUseCase.Dados(1L, "Ana   Souza", true), ADMIN)).hasMessageContaining("Já existe");

        when(repo.listar(1L)).thenReturn(List.of());
        assertThat(uc.validarParaConferencia(1L, "Qualquer Um")).isEqualTo("Qualquer Um");
        when(repo.listar(1L)).thenReturn(List.of(new Conferente(1L, 1L, "Carlos Lima", true), new Conferente(2L, 1L, "Rita Melo", false)));
        assertThat(uc.validarParaConferencia(1L, "carlos  lima")).isEqualTo("Carlos Lima");
        assertThatThrownBy(() -> uc.validarParaConferencia(1L, "Rita Melo")).hasMessageContaining("pausado");
    }

    // ---- WhatsApp e e-mail de cobrança
    @Test
    void linkWaMeNormalizaTelefoneBrasileiroECodificaTexto() {
        assertThat(WhatsAppLink.normalizarTelefone("(11) 98888-7777")).isEqualTo("5511988887777");
        assertThat(WhatsAppLink.normalizarTelefone("+55 11 3333-4444")).isEqualTo("551133334444");
        assertThat(WhatsAppLink.normalizarTelefone("123")).isNull();
        assertThat(WhatsAppLink.link("11 98888-7777", "Olá & até já")).isEqualTo("https://wa.me/5511988887777?text=Ol%C3%A1%20%26%20at%C3%A9%20j%C3%A1");
        assertThat(WhatsAppLink.link(null, "x")).isEqualTo("https://wa.me/?text=x");
    }

    CobrancaPalete cobranca(CobrancaPalete.Status status) {
        return new CobrancaPalete(7L, "Distribuidora <Beta>", 5, new BigDecimal("45"), LocalDate.of(2026, 11, 20), null, "admin",
                OffsetDateTime.now(CLOCK), status, status == CobrancaPalete.Status.PAGA ? "RP-20261102-0001" : null,
                status == CobrancaPalete.Status.PAGA ? CobrancaPalete.FormaPagamento.PIX : null, null, null,
                status == CobrancaPalete.Status.CANCELADA ? "erro" : null);
    }

    NotificacaoUseCase notificacoes(CobrancaPalete c, CanalEmailDocumentos email, NotificadorWhatsApp w, NotificacaoEnvioRepositorio envios) {
        CobrancaPaleteRepositorio cobs = mock(CobrancaPaleteRepositorio.class);
        when(cobs.buscarPorId(7L)).thenReturn(Optional.of(c));
        var paletes = new PaleteFinanceiroUseCase(mock(ValePaleteRepositorio.class), cobs, mock(MovimentoPaleteRepositorio.class),
                mock(CentroDistribuicaoRepositorio.class), auditoria, TX, CLOCK, tipos);
        FornecedorRepositorio forn = mock(FornecedorRepositorio.class);
        when(forn.listar(any(), any())).thenReturn(List.of(new Fornecedor(1L, "11222333000181", "Distribuidora <Beta>", null,
                "cobranca@beta.com.br", "(11) 98888-7777", null, true)));
        when(envios.salvar(any())).thenAnswer(i -> i.getArgument(0));
        return new NotificacaoUseCase(paletes, mock(AgendamentoUseCase.class), forn, email, w, envios, auditoria, CLOCK);
    }

    @Test
    void emailDeCobrancaUsaTemplateEscapadoERegistraOResultado() {
        CanalEmailDocumentos email = mock(CanalEmailDocumentos.class);
        when(email.provedor()).thenReturn("smtp");
        when(email.enviar(anyString(), anyString(), anyString(), anyString())).thenReturn(new CanalEmailDocumentos.Resultado(true, null));
        NotificacaoEnvioRepositorio envios = mock(NotificacaoEnvioRepositorio.class);
        var uc = notificacoes(cobranca(CobrancaPalete.Status.ABERTA), email, mock(NotificadorWhatsApp.class), envios);

        var previa = uc.emailCobranca(7L);
        assertThat(previa.assunto()).startsWith("Cobrança de paletes nº 7");
        assertThat(previa.html()).contains("Distribuidora &lt;Beta&gt;").contains("R$ 225,00").contains("20/11/2026").doesNotContain("<Beta>");
        assertThat(uc.contatoCobranca(7L).email()).isEqualTo("cobranca@beta.com.br");

        NotificacaoEnvio n = uc.enviarEmailCobranca(7L, "Financeiro@Beta.com.br", ADMIN);
        assertThat(n.status()).isEqualTo(NotificacaoEnvio.Status.ENVIADO);
        assertThat(n.destinatario()).isEqualTo("financeiro@beta.com.br");
        assertThat(n.canal()).isEqualTo(NotificacaoEnvio.Canal.EMAIL);
        assertThatThrownBy(() -> uc.enviarEmailCobranca(7L, "invalido", ADMIN)).hasMessageContaining("inválido");

        when(email.enviar(anyString(), anyString(), anyString(), anyString())).thenReturn(new CanalEmailDocumentos.Resultado(false, "SMTP fora"));
        assertThat(uc.enviarEmailCobranca(7L, "a@b.com", ADMIN).status()).isEqualTo(NotificacaoEnvio.Status.FALHOU);
    }

    @Test
    void cobrancaPagaViraReciboECanceladaNaoEnvia() {
        CanalEmailDocumentos email = mock(CanalEmailDocumentos.class);
        var paga = notificacoes(cobranca(CobrancaPalete.Status.PAGA), email, mock(NotificadorWhatsApp.class), mock(NotificacaoEnvioRepositorio.class));
        assertThat(paga.emailCobranca(7L).assunto()).startsWith("Recibo RP-20261102-0001");
        var cancelada = notificacoes(cobranca(CobrancaPalete.Status.CANCELADA), email, mock(NotificadorWhatsApp.class), mock(NotificacaoEnvioRepositorio.class));
        assertThatThrownBy(() -> cancelada.enviarEmailCobranca(7L, "a@b.com", ADMIN)).hasMessageContaining("cancelada");
        verify(email, never()).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void whatsAppSemApiSoGeraLinkEComApiUsaOAdaptador() {
        NotificadorWhatsApp desligado = mock(NotificadorWhatsApp.class);
        when(desligado.habilitado()).thenReturn(false);
        when(desligado.provedor()).thenReturn("wa.me");
        NotificacaoEnvioRepositorio envios = mock(NotificacaoEnvioRepositorio.class);
        var uc = notificacoes(cobranca(CobrancaPalete.Status.ABERTA), mock(CanalEmailDocumentos.class), desligado, envios);
        var m = uc.whatsAppCobranca(7L, null);
        assertThat(m.telefone()).isEqualTo("5511988887777");
        assertThat(m.link()).startsWith("https://wa.me/5511988887777?text=");
        assertThat(m.mensagem()).contains("5 paletes").contains("R$ 225,00").contains("20/11/2026");
        assertThat(uc.registrarWhatsApp(NotificacaoUseCase.DOC_COBRANCA, 7L, null, ADMIN).status())
                .isEqualTo(NotificacaoEnvio.Status.LINK_GERADO);
        verify(desligado, never()).enviar(anyString(), anyString());

        NotificadorWhatsApp api = mock(NotificadorWhatsApp.class);
        when(api.habilitado()).thenReturn(true);
        when(api.provedor()).thenReturn("cloud-api");
        when(api.enviar(eq("5511977776666"), anyString())).thenReturn(new NotificadorWhatsApp.Resultado(true, null));
        var comApi = notificacoes(cobranca(CobrancaPalete.Status.ABERTA), mock(CanalEmailDocumentos.class), api, envios);
        NotificacaoEnvio n = comApi.registrarWhatsApp(NotificacaoUseCase.DOC_COBRANCA, 7L, "11 97777-6666", ADMIN);
        assertThat(n.status()).isEqualTo(NotificacaoEnvio.Status.ENVIADO);
        assertThat(n.provedor()).isEqualTo("cloud-api");
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(api).enviar(eq("5511977776666"), texto.capture());
        assertThat(texto.getValue()).contains("cobrança de paletes nº 7");
    }
}
