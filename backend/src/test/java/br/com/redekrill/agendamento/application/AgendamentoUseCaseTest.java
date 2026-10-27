package br.com.redekrill.agendamento.application;

import br.com.redekrill.agendamento.application.dto.AgendamentoDetalhe;
import br.com.redekrill.agendamento.application.dto.FiltroAgendamento;
import br.com.redekrill.agendamento.application.dto.NovoAgendamento;
import br.com.redekrill.agendamento.application.dto.ResultadoAgendamento;
import br.com.redekrill.agendamento.application.dto.Simulacao;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AgendamentoRepositorio;
import br.com.redekrill.agendamento.application.port.FaixaHorariaRepositorio;
import br.com.redekrill.agendamento.application.port.NotificadorEmail;
import br.com.redekrill.agendamento.application.port.NotificadorPush;
import br.com.redekrill.agendamento.application.port.ParametroRepositorio;
import br.com.redekrill.agendamento.application.port.TipoCaminhaoRepositorio;
import br.com.redekrill.agendamento.application.port.TipoPaletizacaoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.application.usecase.AgendamentoUseCase;
import br.com.redekrill.agendamento.application.usecase.AuditoriaUseCase;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.AlertaCapacidade;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.OcupacaoDia;
import br.com.redekrill.agendamento.domain.model.ParametroCapacidade;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import br.com.redekrill.agendamento.domain.model.TipoCaminhao;
import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendamentoUseCaseTest {

    static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), SP);
    static final LocalDate DIA = LocalDate.of(2026, 11, 3);
    static final FaixaHoraria F1 = new FaixaHoraria(1L, LocalTime.of(8, 0), LocalTime.of(10, 0), 120, true);
    static final FaixaHoraria F2 = new FaixaHoraria(2L, LocalTime.of(10, 0), LocalTime.of(12, 0), 120, true);
    static final FaixaHoraria F_INATIVA = new FaixaHoraria(3L, LocalTime.of(13, 0), LocalTime.of(15, 0), 120, false);
    static final TipoCaminhao TRUCK = new TipoCaminhao(1L, "Truck", null, 14, true);
    static final TipoPaletizacao PAL = new TipoPaletizacao(1L, "Paletizado", 1, true);
    static final TipoPaletizacao NAO_PAL = new TipoPaletizacao(2L, "Não paletizado", 2, true);
    static final Solicitante ADMIN = new Solicitante("admin", "10.0.0.1");

    @Mock AgendamentoRepositorio agendamentos;
    @Mock FaixaHorariaRepositorio faixas;
    @Mock TipoCaminhaoRepositorio caminhoes;
    @Mock TipoPaletizacaoRepositorio paletizacoes;
    @Mock ParametroRepositorio parametros;
    @Mock NotificadorEmail email;
    @Mock NotificadorPush push;
    @Mock br.com.redekrill.agendamento.application.port.AuditoriaRepositorio auditoriaRepo;

    final Transacao tx = new Transacao() {
        @Override
        public <T> T executar(Supplier<T> op) {
            return op.get();
        }
    };
    final List<Agendamento> banco = new ArrayList<>();
    AgendamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AgendamentoUseCase(agendamentos, faixas, caminhoes, paletizacoes, parametros, email, push,
                new AuditoriaUseCase(auditoriaRepo, CLOCK), tx, CLOCK);
        when(parametros.obter()).thenReturn(ParametroCapacidade.PADRAO);
        when(faixas.listar()).thenReturn(List.of(F1, F2, F_INATIVA));
        when(faixas.buscarPorId(1L)).thenReturn(Optional.of(F1));
        when(faixas.buscarPorId(2L)).thenReturn(Optional.of(F2));
        when(caminhoes.listar()).thenReturn(List.of(TRUCK));
        when(caminhoes.buscarPorId(1L)).thenReturn(Optional.of(TRUCK));
        when(paletizacoes.listar()).thenReturn(List.of(PAL, NAO_PAL));
        when(paletizacoes.buscarPorId(1L)).thenReturn(Optional.of(PAL));
        when(paletizacoes.buscarPorId(2L)).thenReturn(Optional.of(NAO_PAL));
        when(agendamentos.listarPorPeriodo(any(), any())).thenAnswer(i -> List.copyOf(banco));
        when(agendamentos.salvar(any())).thenAnswer(i -> {
            Agendamento a = i.getArgument(0);
            Agendamento salvo = Agendamento.builder().id(a.getId() == null ? banco.size() + 1L : a.getId())
                    .data(a.getData()).faixaHorariaId(a.getFaixaHorariaId()).idPedido(a.getIdPedido())
                    .fornecedor(a.getFornecedor()).emailFornecedor(a.getEmailFornecedor())
                    .tipoCaminhaoId(a.getTipoCaminhaoId()).tipoPaletizacaoId(a.getTipoPaletizacaoId())
                    .quantidadePaletes(a.getQuantidadePaletes()).paletesEquivalentes(a.getPaletesEquivalentes())
                    .observacao(a.getObservacao()).status(a.getStatus()).criadoPor(a.getCriadoPor())
                    .criadoEm(a.getCriadoEm()).canceladoPor(a.getCanceladoPor()).canceladoEm(a.getCanceladoEm())
                    .motivoCancelamento(a.getMotivoCancelamento()).build();
            banco.removeIf(b -> b.getId().equals(salvo.getId()));
            banco.add(salvo);
            return salvo;
        });
        when(agendamentos.buscarPorId(any())).thenAnswer(i -> banco.stream()
                .filter(b -> b.getId().equals(i.getArgument(0))).findFirst());
    }

    NovoAgendamento cmd(long faixa, long paletizacao, int qtd) {
        return new NovoAgendamento(DIA, faixa, 4500L, "Fornecedor Alfa", "alfa@fornecedor.com", 1L, paletizacao, qtd, "Doca 2");
    }

    @Test
    void criaSemAlertaEnviaEmailEPush() {
        ResultadoAgendamento r = useCase.criar(cmd(1, 1, 50), ADMIN);
        assertThat(r.alertas()).isEmpty();
        assertThat(r.agendamento().faixaDescricao()).isEqualTo("08:00 - 10:00");
        assertThat(r.agendamento().tipoCaminhao()).isEqualTo("Truck");
        assertThat(r.agendamento().agendamento().getCriadoPor()).isEqualTo("admin");
        InOrder ordem = inOrder(agendamentos);
        ordem.verify(agendamentos).bloquearDia(DIA);
        ordem.verify(agendamentos).salvar(any());
        verify(email).enviarConfirmacao(r.agendamento());
        verify(push).notificarTodos(eq("Novo agendamento"), contains("Fornecedor Alfa"), eq("/agendamentos/1"));
        verify(push, never()).notificarTodos(eq("Capacidade excedida"), anyString(), anyString());
        ArgumentCaptor<br.com.redekrill.agendamento.application.dto.RegistroAuditoria> aud =
                ArgumentCaptor.forClass(br.com.redekrill.agendamento.application.dto.RegistroAuditoria.class);
        verify(auditoriaRepo).registrar(aud.capture());
        assertThat(aud.getValue().acao()).isEqualTo("CRIAR");
        assertThat(aud.getValue().ip()).isEqualTo("10.0.0.1");
        assertThat(aud.getValue().entidadeId()).isEqualTo("1");
    }

    @Test
    void naoPaletizadoDobraEExcedenteSoAvisaSemBloquear() {
        useCase.criar(cmd(1, 1, 100), ADMIN);
        ResultadoAgendamento r = useCase.criar(cmd(1, 2, 20), ADMIN);
        assertThat(r.agendamento().agendamento().getPaletesEquivalentes()).isEqualTo(40);
        assertThat(r.alertas()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AlertaCapacidade.Tipo.FAIXA);
            assertThat(a.excedente()).isEqualTo(20);
        });
        assertThat(banco).hasSize(2);
        verify(push).notificarTodos(eq("Capacidade excedida"), contains("excedida"), eq("/painel?data=" + DIA));
    }

    @Test
    void alertaDiarioQuandoPassaDe480() {
        useCase.criar(cmd(1, 2, 120), ADMIN); // 240
        useCase.criar(cmd(2, 2, 120), ADMIN); // 480
        ResultadoAgendamento r = useCase.criar(cmd(2, 1, 1), ADMIN);
        assertThat(r.alertas()).extracting(AlertaCapacidade::tipo)
                .containsExactly(AlertaCapacidade.Tipo.DIA, AlertaCapacidade.Tipo.FAIXA);
    }

    @Test
    void validacoesDeEntrada() {
        assertThatThrownBy(() -> useCase.criar(new NovoAgendamento(null, 1L, 1L, "F", "a@b.com", 1L, 1L, 1, null), ADMIN))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> useCase.criar(new NovoAgendamento(DIA, 1L, 1L, "F", "a@b.com", 1L, 1L, null, null), ADMIN))
                .hasMessageContaining("Quantidade");
        assertThatThrownBy(() -> useCase.criar(new NovoAgendamento(DIA, null, 1L, "F", "a@b.com", 1L, 1L, 1, null), ADMIN))
                .hasMessageContaining("Faixa");
        assertThatThrownBy(() -> useCase.criar(new NovoAgendamento(DIA, 1L, 1L, "F", "a@b.com", null, 1L, 1, null), ADMIN))
                .hasMessageContaining("caminhão");
        assertThatThrownBy(() -> useCase.criar(new NovoAgendamento(DIA, 1L, 1L, "F", "a@b.com", 1L, null, 1, null), ADMIN))
                .hasMessageContaining("paletização");
        assertThatThrownBy(() -> useCase.criar(new NovoAgendamento(DIA, 9L, 1L, "F", "a@b.com", 1L, 1L, 1, null), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> useCase.criar(new NovoAgendamento(DIA, 1L, 1L, "F", "a@b.com", 9L, 1L, 1, null), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> useCase.criar(new NovoAgendamento(DIA, 1L, 1L, "F", "a@b.com", 1L, 9L, 1, null), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(email, never()).enviarConfirmacao(any());
    }

    @Test
    void cancelaLiberaCapacidadeENotifica() {
        Long id = useCase.criar(cmd(1, 1, 100), ADMIN).agendamento().agendamento().getId();
        AgendamentoDetalhe d = useCase.cancelar(id, "Fornecedor sem nota", ADMIN);
        assertThat(d.agendamento().getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(useCase.ocupacaoDoDia(DIA).paletesOcupados()).isZero();
        verify(email).enviarCancelamento(d);
        verify(push).notificarTodos(eq("Agendamento cancelado"), anyString(), eq("/agendamentos/" + id));
        verify(auditoriaRepo, times(2)).registrar(any());
    }

    @Test
    void cancelarInexistente() {
        assertThatThrownBy(() -> useCase.cancelar(99L, "x", ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> useCase.buscar(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscaEListaComDescricoes() {
        useCase.criar(cmd(2, 2, 10), ADMIN);
        banco.add(AgendamentoTestData.orfao());
        FiltroAgendamento f = new FiltroAgendamento(DIA, DIA, null, null, null, null);
        when(agendamentos.listar(f)).thenAnswer(i -> List.copyOf(banco));
        List<AgendamentoDetalhe> lista = useCase.listar(f);
        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).tipoPaletizacao()).isEqualTo("Não paletizado");
        assertThat(lista.get(1).faixaDescricao()).isEqualTo("-");
        assertThat(lista.get(1).tipoCaminhao()).isEqualTo("-");
        assertThat(lista.get(1).tipoPaletizacao()).isEqualTo("-");
        assertThat(useCase.buscar(1L).faixaDescricao()).isEqualTo("10:00 - 12:00");
    }

    @Test
    void periodoInvalido() {
        assertThatThrownBy(() -> useCase.listar(new FiltroAgendamento(DIA, DIA.minusDays(1), null, null, null, null)))
                .hasMessageContaining("anterior");
        assertThatThrownBy(() -> useCase.ocupacaoPeriodo(DIA, DIA.plusDays(400))).hasMessageContaining("máximo");
        assertThatThrownBy(() -> useCase.ocupacaoPeriodo(null, DIA)).hasMessageContaining("Informe");
        assertThatThrownBy(() -> useCase.ocupacaoPeriodo(DIA, null)).hasMessageContaining("Informe");
        assertThat(useCase.listar(new FiltroAgendamento(null, null, null, null, null, null))).isEmpty();
        assertThat(useCase.listar(new FiltroAgendamento(DIA, null, null, null, null, null))).isEmpty();
    }

    @Test
    void ocupacaoPorPeriodoIgnoraFaixaInativa() {
        useCase.criar(cmd(1, 1, 60), ADMIN);
        List<OcupacaoDia> dias = useCase.ocupacaoPeriodo(DIA.minusDays(1), DIA.plusDays(1));
        assertThat(dias).hasSize(3);
        assertThat(dias.get(1).paletesOcupados()).isEqualTo(60);
        assertThat(dias.get(1).percentual()).isEqualTo(12.5);
        assertThat(dias.get(1).faixas()).hasSize(2);
        assertThat(dias.get(0).paletesOcupados()).isZero();
    }

    @Test
    void simulacao() {
        useCase.criar(cmd(1, 1, 110), ADMIN);
        Simulacao s = useCase.simular(DIA, 1L, 2L, 10);
        assertThat(s.paletesEquivalentes()).isEqualTo(20);
        assertThat(s.alertas()).hasSize(1);
        assertThat(useCase.simular(DIA, null, null, 0).alertas()).isEmpty();
        assertThat(useCase.simular(DIA, 1L, null, 5).paletesEquivalentes()).isEqualTo(5);
        assertThat(useCase.simular(DIA, 1L, 2L, -3).paletesEquivalentes()).isZero();
        assertThatThrownBy(() -> useCase.simular(null, 1L, 1L, 1)).isInstanceOf(RegraNegocioException.class);
    }

    static final class AgendamentoTestData {
        static Agendamento orfao() {
            return Agendamento.builder().id(77L).data(DIA).faixaHorariaId(50L).idPedido(1L).fornecedor("F")
                    .emailFornecedor("f@f.com").tipoCaminhaoId(50L).tipoPaletizacaoId(50L).quantidadePaletes(1)
                    .paletesEquivalentes(1).build();
        }
    }

    @Test
    void auditoriaComDetalhesNulosESolicitanteNulo() {
        AuditoriaUseCase a = new AuditoriaUseCase(auditoriaRepo, CLOCK);
        a.registrar(null, "LOGIN_FALHA", "SESSAO", null, false, null);
        ArgumentCaptor<br.com.redekrill.agendamento.application.dto.RegistroAuditoria> c =
                ArgumentCaptor.forClass(br.com.redekrill.agendamento.application.dto.RegistroAuditoria.class);
        verify(auditoriaRepo).registrar(c.capture());
        assertThat(c.getValue().usuario()).isEqualTo("anonimo");
        assertThat(c.getValue().detalhes()).isEqualTo(Map.of());
        assertThat(c.getValue().entidadeId()).isNull();
        assertThat(c.getValue().ip()).isNull();
        assertThat(c.getValue().sucesso()).isFalse();
    }
}
