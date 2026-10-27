package br.com.redekrill.agendamento.domain;

import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.ParametroCapacidade;
import br.com.redekrill.agendamento.domain.model.TipoCaminhao;
import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;
import br.com.redekrill.agendamento.domain.service.ValidadorFaixas;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadastrosDominioTest {

    static FaixaHoraria faixa(Long id, int h, boolean ativo) {
        return new FaixaHoraria(id, LocalTime.of(h, 0), LocalTime.of(h + 2, 0), 120, ativo);
    }

    @Test
    void faixaValida() {
        FaixaHoraria f = faixa(1L, 8, true);
        assertThat(f.descricao()).isEqualTo("08:00 - 10:00");
        assertThat(f.duracaoMinutos()).isEqualTo(120);
        assertThat(f.sobrepoe(faixa(2L, 9, true))).isTrue();
        assertThat(f.sobrepoe(faixa(3L, 10, true))).isFalse();
    }

    @Test
    void faixaInvalida() {
        assertThatThrownBy(() -> new FaixaHoraria(1L, null, LocalTime.NOON, 1, true)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new FaixaHoraria(1L, LocalTime.NOON, null, 1, true)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new FaixaHoraria(1L, LocalTime.NOON, LocalTime.NOON, 1, true)).hasMessageContaining("posterior");
        assertThatThrownBy(() -> new FaixaHoraria(1L, LocalTime.of(8, 0), LocalTime.NOON, 0, true)).hasMessageContaining("Capacidade");
    }

    @Test
    void tiposValidam() {
        assertThat(new TipoCaminhao(1L, " Truck ", "d", 14, true).nome()).isEqualTo("Truck");
        assertThat(new TipoCaminhao(1L, "Van", null, null, true).capacidadePaletes()).isNull();
        assertThatThrownBy(() -> new TipoCaminhao(1L, " ", null, null, true)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new TipoCaminhao(1L, null, null, null, true)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new TipoCaminhao(1L, "X", null, 0, true)).isInstanceOf(RegraNegocioException.class);
        assertThat(new TipoPaletizacao(1L, " Paletizado ", 1, true).nome()).isEqualTo("Paletizado");
        assertThatThrownBy(() -> new TipoPaletizacao(1L, "", 1, true)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new TipoPaletizacao(1L, null, 1, true)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new TipoPaletizacao(1L, "X", 0, true)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void parametroValida() {
        assertThat(ParametroCapacidade.PADRAO.capacidadeDiariaPaletes()).isEqualTo(480);
        assertThatThrownBy(() -> new ParametroCapacidade(0, 4, 120)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new ParametroCapacidade(480, 0, 120)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new ParametroCapacidade(480, 4, 10)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void validadorFaixas() {
        ValidadorFaixas v = new ValidadorFaixas();
        List<FaixaHoraria> existentes = List.of(faixa(1L, 8, true), faixa(2L, 10, true), faixa(3L, 13, true), faixa(4L, 15, false));
        ParametroCapacidade p = ParametroCapacidade.PADRAO;
        assertThatCode(() -> v.validar(faixa(null, 15, true), existentes, p)).doesNotThrowAnyException();
        assertThatCode(() -> v.validar(faixa(1L, 8, true), existentes, p)).doesNotThrowAnyException();
        assertThatCode(() -> v.validar(faixa(null, 9, false), existentes, p)).doesNotThrowAnyException();
        assertThatThrownBy(() -> v.validar(faixa(null, 9, true), existentes, p)).hasMessageContaining("sobrepõe");
        assertThatThrownBy(() -> v.validar(new FaixaHoraria(null, LocalTime.of(17, 0), LocalTime.of(18, 0), 1, true), existentes, p))
                .hasMessageContaining("duração");
        List<FaixaHoraria> quatro = List.of(faixa(1L, 6, true), faixa(2L, 8, true), faixa(3L, 10, true), faixa(4L, 12, true));
        assertThatThrownBy(() -> v.validar(faixa(null, 15, true), quatro, p)).hasMessageContaining("Limite");
    }

    @Test
    void excecaoNaoEncontrado() {
        assertThat(new RecursoNaoEncontradoException("Agendamento", 5).getMessage()).isEqualTo("Agendamento não encontrado(a): 5");
    }
}
