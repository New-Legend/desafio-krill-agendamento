package br.com.redekrill.agendamento.application;

import br.com.redekrill.agendamento.application.dto.FiltroAuditoria;
import br.com.redekrill.agendamento.application.dto.Pagina;
import br.com.redekrill.agendamento.application.dto.Solicitante;
import br.com.redekrill.agendamento.application.port.AuditoriaRepositorio;
import br.com.redekrill.agendamento.application.port.FaixaHorariaRepositorio;
import br.com.redekrill.agendamento.application.port.ParametroRepositorio;
import br.com.redekrill.agendamento.application.port.TipoCaminhaoRepositorio;
import br.com.redekrill.agendamento.application.port.TipoPaletizacaoRepositorio;
import br.com.redekrill.agendamento.application.port.Transacao;
import br.com.redekrill.agendamento.application.usecase.AuditoriaUseCase;
import br.com.redekrill.agendamento.application.usecase.CadastroUseCase;
import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.ParametroCapacidade;
import br.com.redekrill.agendamento.domain.model.TipoCaminhao;
import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CadastroUseCaseTest {

    @Mock ParametroRepositorio parametros;
    @Mock FaixaHorariaRepositorio faixas;
    @Mock TipoCaminhaoRepositorio caminhoes;
    @Mock TipoPaletizacaoRepositorio paletizacoes;
    @Mock AuditoriaRepositorio auditoriaRepo;
    final Solicitante admin = new Solicitante("admin", "127.0.0.1");
    CadastroUseCase useCase;

    static FaixaHoraria f(Long id, int h, boolean ativo) {
        return new FaixaHoraria(id, LocalTime.of(h, 0), LocalTime.of(h + 2, 0), 120, ativo);
    }

    @BeforeEach
    void setUp() {
        Transacao tx = new Transacao() {
            @Override
            public <T> T executar(Supplier<T> op) {
                return op.get();
            }
        };
        useCase = new CadastroUseCase(parametros, faixas, caminhoes, paletizacoes,
                new AuditoriaUseCase(auditoriaRepo, Clock.systemUTC()), tx);
        when(parametros.obter()).thenReturn(ParametroCapacidade.PADRAO);
        when(parametros.salvar(any())).thenAnswer(i -> i.getArgument(0));
        when(faixas.listar()).thenReturn(List.of(f(2L, 10, true), f(1L, 8, true), f(3L, 13, false)));
        when(faixas.salvar(any())).thenAnswer(i -> {
            FaixaHoraria x = i.getArgument(0);
            return new FaixaHoraria(x.id() == null ? 10L : x.id(), x.horaInicio(), x.horaFim(), x.capacidadePaletes(), x.ativo());
        });
        when(caminhoes.salvar(any())).thenAnswer(i -> {
            TipoCaminhao x = i.getArgument(0);
            return new TipoCaminhao(x.id() == null ? 10L : x.id(), x.nome(), x.descricao(), x.capacidadePaletes(), x.ativo());
        });
        when(paletizacoes.salvar(any())).thenAnswer(i -> {
            TipoPaletizacao x = i.getArgument(0);
            return new TipoPaletizacao(x.id() == null ? 10L : x.id(), x.nome(), x.fatorMultiplicador(), x.ativo());
        });
    }

    @Test
    void parametro() {
        assertThat(useCase.obterParametro()).isEqualTo(ParametroCapacidade.PADRAO);
        ParametroCapacidade novo = new ParametroCapacidade(600, 4, 120);
        assertThat(useCase.atualizarParametro(novo, admin)).isEqualTo(novo);
        verify(auditoriaRepo).registrar(any());
        assertThatThrownBy(() -> useCase.atualizarParametro(new ParametroCapacidade(600, 1, 120), admin))
                .hasMessageContaining("inative");
    }

    @Test
    void faixas() {
        assertThat(useCase.listarFaixas()).extracting(FaixaHoraria::id).containsExactly(1L, 2L, 3L);
        assertThat(useCase.salvarFaixa(f(null, 15, true), admin).id()).isEqualTo(10L);
        when(faixas.buscarPorId(1L)).thenReturn(Optional.of(f(1L, 8, true)));
        assertThat(useCase.salvarFaixa(new FaixaHoraria(1L, LocalTime.of(8, 0), LocalTime.of(10, 0), 150, true), admin)
                .capacidadePaletes()).isEqualTo(150);
        assertThatThrownBy(() -> useCase.salvarFaixa(f(99L, 15, true), admin)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> useCase.salvarFaixa(f(null, 9, true), admin)).hasMessageContaining("sobrepõe");
    }

    @Test
    void tiposCaminhao() {
        when(caminhoes.listar()).thenReturn(List.of(new TipoCaminhao(2L, "Van", null, 4, true), new TipoCaminhao(1L, "Carreta", null, 28, true)));
        assertThat(useCase.listarTiposCaminhao()).extracting(TipoCaminhao::nome).containsExactly("Carreta", "Van");
        assertThat(useCase.salvarTipoCaminhao(new TipoCaminhao(null, "Bitrem", null, 40, true), admin).id()).isEqualTo(10L);
        when(caminhoes.existeNome(eq("Van"), any())).thenReturn(true);
        assertThatThrownBy(() -> useCase.salvarTipoCaminhao(new TipoCaminhao(null, "Van", null, 4, true), admin))
                .hasMessageContaining("Já existe");
        assertThatThrownBy(() -> useCase.salvarTipoCaminhao(new TipoCaminhao(5L, "Toco", null, 4, true), admin))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        when(caminhoes.buscarPorId(1L)).thenReturn(Optional.of(new TipoCaminhao(1L, "Carreta", null, 28, true)));
        assertThat(useCase.salvarTipoCaminhao(new TipoCaminhao(1L, "Carreta LS", null, 30, false), admin).ativo()).isFalse();
    }

    @Test
    void tiposPaletizacao() {
        when(paletizacoes.listar()).thenReturn(List.of(new TipoPaletizacao(2L, "Não paletizado", 2, true), new TipoPaletizacao(1L, "Granel", 3, true)));
        assertThat(useCase.listarTiposPaletizacao()).extracting(TipoPaletizacao::nome).containsExactly("Granel", "Não paletizado");
        assertThat(useCase.salvarTipoPaletizacao(new TipoPaletizacao(null, "Caixa solta", 3, true), admin).fatorMultiplicador()).isEqualTo(3);
        when(paletizacoes.existeNome(eq("Granel"), any())).thenReturn(true);
        assertThatThrownBy(() -> useCase.salvarTipoPaletizacao(new TipoPaletizacao(null, "Granel", 1, true), admin))
                .hasMessageContaining("Já existe");
        assertThatThrownBy(() -> useCase.salvarTipoPaletizacao(new TipoPaletizacao(7L, "Outro", 1, true), admin))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        when(paletizacoes.buscarPorId(2L)).thenReturn(Optional.of(new TipoPaletizacao(2L, "Não paletizado", 2, true)));
        assertThat(useCase.salvarTipoPaletizacao(new TipoPaletizacao(2L, "Não paletizado", 2, false), admin).ativo()).isFalse();
        verify(paletizacoes, never()).salvar(eq(new TipoPaletizacao(null, "Granel", 1, true)));
    }

    @Test
    void auditoriaLimitaPaginacao() {
        AuditoriaUseCase a = new AuditoriaUseCase(auditoriaRepo, Clock.systemUTC());
        FiltroAuditoria filtro = new FiltroAuditoria(null, null, null, null, null);
        when(auditoriaRepo.listar(any(), eq(0), eq(200))).thenReturn(new Pagina<>(List.of(), 0, 0, 200));
        when(auditoriaRepo.listar(any(), eq(0), eq(1))).thenReturn(new Pagina<>(List.of(), 0, 0, 1));
        assertThat(a.listar(filtro, -1, 5000).tamanho()).isEqualTo(200);
        assertThat(a.listar(filtro, 0, 0).tamanho()).isEqualTo(1);
    }
}
