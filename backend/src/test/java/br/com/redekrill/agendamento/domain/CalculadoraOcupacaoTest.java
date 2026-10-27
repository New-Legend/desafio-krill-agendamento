package br.com.redekrill.agendamento.domain;

import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.AlertaCapacidade;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.OcupacaoDia;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import br.com.redekrill.agendamento.domain.service.CalculadoraOcupacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraOcupacaoTest {

    static final LocalDate DIA = LocalDate.of(2026, 11, 3);
    static final FaixaHoraria F1 = new FaixaHoraria(1L, LocalTime.of(8, 0), LocalTime.of(10, 0), 120, true);
    static final FaixaHoraria F2 = new FaixaHoraria(2L, LocalTime.of(10, 0), LocalTime.of(12, 0), 120, true);
    final CalculadoraOcupacao calc = new CalculadoraOcupacao();

    static Agendamento ag(LocalDate d, long faixa, int equivalentes, StatusAgendamento s) {
        return AgendamentoTest.base().data(d).faixaHorariaId(faixa).quantidadePaletes(equivalentes)
                .paletesEquivalentes(equivalentes).status(s).build();
    }

    @Test
    void somaSomenteAtivosDoDiaEPorFaixa() {
        OcupacaoDia o = calc.calcular(DIA, 480, List.of(F2, F1), List.of(
                ag(DIA, 1, 100, StatusAgendamento.AGENDADO),
                ag(DIA, 1, 40, StatusAgendamento.AGENDADO),
                ag(DIA, 2, 60, StatusAgendamento.AGENDADO),
                ag(DIA, 2, 300, StatusAgendamento.CANCELADO),
                ag(DIA.plusDays(1), 1, 200, StatusAgendamento.AGENDADO)));
        assertThat(o.paletesOcupados()).isEqualTo(200);
        assertThat(o.quantidadeAgendamentos()).isEqualTo(3);
        assertThat(o.percentual()).isEqualTo(41.7);
        assertThat(o.excedente()).isZero();
        assertThat(o.faixas()).extracting(f -> f.faixa().id()).containsExactly(1L, 2L);
        assertThat(o.faixas().get(0).paletesOcupados()).isEqualTo(140);
        assertThat(o.faixas().get(0).percentual()).isEqualTo(116.7);
        assertThat(o.faixas().get(0).excedente()).isEqualTo(20);
        assertThat(o.faixas().get(1).excedente()).isZero();
    }

    @Test
    void naoGeraAlertaDentroDoLimite() {
        OcupacaoDia o = calc.calcular(DIA, 480, List.of(F1), List.of(ag(DIA, 1, 100, StatusAgendamento.AGENDADO)));
        assertThat(calc.avaliar(o, 1L, 20)).isEmpty();
    }

    @Test
    void alertaFaixaEDiaQuandoExcede() {
        OcupacaoDia o = calc.calcular(DIA, 480, List.of(F1, F2), List.of(
                ag(DIA, 1, 110, StatusAgendamento.AGENDADO), ag(DIA, 2, 360, StatusAgendamento.AGENDADO)));
        List<AlertaCapacidade> alertas = calc.avaliar(o, 1L, 20);
        assertThat(alertas).hasSize(2);
        assertThat(alertas.get(0).tipo()).isEqualTo(AlertaCapacidade.Tipo.DIA);
        assertThat(alertas.get(0).excedente()).isEqualTo(10);
        assertThat(alertas.get(0).ocupacaoResultante()).isEqualTo(490);
        assertThat(alertas.get(1).tipo()).isEqualTo(AlertaCapacidade.Tipo.FAIXA);
        assertThat(alertas.get(1).excedente()).isEqualTo(10);
        assertThat(alertas.get(1).mensagem()).contains("08:00 - 10:00");
    }

    @Test
    void faixaDesconhecidaAvaliaSoODia() {
        OcupacaoDia o = calc.calcular(DIA, 10, List.of(F1), List.of());
        assertThat(calc.avaliar(o, 99L, 20)).singleElement().extracting(AlertaCapacidade::tipo).isEqualTo(AlertaCapacidade.Tipo.DIA);
    }

    @Test
    void capacidadeZeroNaoDivide() {
        OcupacaoDia o = new OcupacaoDia(DIA, 0, 10, 1, List.of());
        assertThat(o.percentual()).isZero();
    }
}
