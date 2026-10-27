package br.com.redekrill.agendamento.domain;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import br.com.redekrill.agendamento.domain.model.TipoCaminhao;
import br.com.redekrill.agendamento.domain.model.TipoPaletizacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgendamentoTest {

    static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 11, 2, 9, 0, 0, 0, ZoneOffset.ofHours(-3));
    static final LocalDate AMANHA = LocalDate.of(2026, 11, 3);
    static final FaixaHoraria FAIXA = new FaixaHoraria(1L, LocalTime.of(8, 0), LocalTime.of(10, 0), 120, true);
    static final TipoCaminhao TRUCK = new TipoCaminhao(1L, "Truck", null, 14, true);
    static final TipoPaletizacao PALETIZADO = new TipoPaletizacao(1L, "Paletizado", 1, true);
    static final TipoPaletizacao NAO_PALETIZADO = new TipoPaletizacao(2L, "Não paletizado", 2, true);

    static Agendamento novo(TipoPaletizacao p, int qtd) {
        return Agendamento.novo(AMANHA, FAIXA, 10L, " Fornecedor X ", "Compras@Fornecedor.com.br", TRUCK, p, qtd,
                "  ", "admin", AGORA);
    }

    @Test
    void naoPaletizadoContaEmDobro() {
        assertThat(novo(NAO_PALETIZADO, 30).getPaletesEquivalentes()).isEqualTo(60);
        assertThat(novo(PALETIZADO, 30).getPaletesEquivalentes()).isEqualTo(30);
    }

    @Test
    void normalizaCampos() {
        Agendamento a = novo(PALETIZADO, 5);
        assertThat(a.getFornecedor()).isEqualTo("Fornecedor X");
        assertThat(a.getEmailFornecedor()).isEqualTo("compras@fornecedor.com.br");
        assertThat(a.getObservacao()).isNull();
        assertThat(a.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        assertThat(a.ativo()).isTrue();
        assertThat(a.getCriadoPor()).isEqualTo("admin");
        assertThat(a.getCriadoEm()).isEqualTo(AGORA);
        assertThat(a.getTipoCaminhaoId()).isEqualTo(1L);
        assertThat(a.getTipoPaletizacaoId()).isEqualTo(1L);
        assertThat(a.getFaixaHorariaId()).isEqualTo(1L);
        assertThat(a.getIdPedido()).isEqualTo(10L);
        assertThat(a.getData()).isEqualTo(AMANHA);
        assertThat(a.getQuantidadePaletes()).isEqualTo(5);
    }

    @Test
    void cancelaComMotivo() {
        Agendamento a = novo(PALETIZADO, 5);
        a.cancelar("admin", " fornecedor desistiu ", AGORA);
        assertThat(a.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(a.ativo()).isFalse();
        assertThat(a.getMotivoCancelamento()).isEqualTo("fornecedor desistiu");
        assertThat(a.getCanceladoPor()).isEqualTo("admin");
        assertThat(a.getCanceladoEm()).isEqualTo(AGORA);
        assertThatThrownBy(() -> a.cancelar("admin", "x", AGORA)).hasMessageContaining("já está cancelado");
    }

    @Test
    void cancelamentoExigeMotivo() {
        Agendamento a = novo(PALETIZADO, 5);
        assertThatThrownBy(() -> a.cancelar("admin", " ", AGORA)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> a.cancelar("admin", null, AGORA)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void rejeitaDataPassada() {
        assertThatThrownBy(() -> Agendamento.novo(AMANHA.minusDays(5), FAIXA, 1L, "F", "a@b.com", TRUCK, PALETIZADO, 1,
                null, "admin", AGORA)).hasMessageContaining("anterior a hoje");
    }

    @Test
    void rejeitaCadastrosInativos() {
        FaixaHoraria inativa = new FaixaHoraria(2L, LocalTime.of(8, 0), LocalTime.of(10, 0), 120, false);
        assertThatThrownBy(() -> Agendamento.novo(AMANHA, inativa, 1L, "F", "a@b.com", TRUCK, PALETIZADO, 1, null, "a", AGORA))
                .hasMessageContaining("Faixa");
        assertThatThrownBy(() -> Agendamento.novo(AMANHA, null, 1L, "F", "a@b.com", TRUCK, PALETIZADO, 1, null, "a", AGORA))
                .hasMessageContaining("Faixa");
        TipoCaminhao camInativo = new TipoCaminhao(2L, "Van", null, null, false);
        assertThatThrownBy(() -> Agendamento.novo(AMANHA, FAIXA, 1L, "F", "a@b.com", camInativo, PALETIZADO, 1, null, "a", AGORA))
                .hasMessageContaining("caminhão");
        assertThatThrownBy(() -> Agendamento.novo(AMANHA, FAIXA, 1L, "F", "a@b.com", null, PALETIZADO, 1, null, "a", AGORA))
                .hasMessageContaining("caminhão");
        TipoPaletizacao palInativa = new TipoPaletizacao(3L, "X", 1, false);
        assertThatThrownBy(() -> Agendamento.novo(AMANHA, FAIXA, 1L, "F", "a@b.com", TRUCK, palInativa, 1, null, "a", AGORA))
                .hasMessageContaining("paletização");
        assertThatThrownBy(() -> Agendamento.novo(AMANHA, FAIXA, 1L, "F", "a@b.com", TRUCK, null, 1, null, "a", AGORA))
                .hasMessageContaining("paletização");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sem-arroba", "a@b", "@b.com", "a b@c.com"})
    void rejeitaEmailInvalido(String email) {
        assertThatThrownBy(() -> base().emailFornecedor(email).build()).hasMessageContaining("E-mail do fornecedor inválido");
    }

    @Test
    void validaCamposObrigatoriosETamanho() {
        assertThatThrownBy(() -> base().data(null).build()).hasMessageContaining("Data");
        assertThatThrownBy(() -> base().faixaHorariaId(null).build()).hasMessageContaining("Faixa");
        assertThatThrownBy(() -> base().idPedido(null).build()).hasMessageContaining("pedido");
        assertThatThrownBy(() -> base().idPedido(0L).build()).hasMessageContaining("pedido");
        assertThatThrownBy(() -> base().fornecedor(null).build()).hasMessageContaining("Fornecedor é obrigatório");
        assertThatThrownBy(() -> base().fornecedor(" ").build()).hasMessageContaining("Fornecedor é obrigatório");
        assertThatThrownBy(() -> base().fornecedor("x".repeat(81)).build()).hasMessageContaining("80");
        assertThatThrownBy(() -> base().emailFornecedor(null).build()).hasMessageContaining("E-mail");
        assertThatThrownBy(() -> base().emailFornecedor("a".repeat(75) + "@b.com").build()).hasMessageContaining("80");
        assertThatThrownBy(() -> base().tipoCaminhaoId(null).build()).hasMessageContaining("caminhão");
        assertThatThrownBy(() -> base().tipoPaletizacaoId(null).build()).hasMessageContaining("paletização");
        assertThatThrownBy(() -> base().quantidadePaletes(0).build()).hasMessageContaining("Quantidade");
        assertThat(base().fornecedor("x".repeat(80)).build().getFornecedor()).hasSize(80);
    }

    @Test
    void reconstroiAgendamentoCancelado() {
        Agendamento a = base().id(9L).status(StatusAgendamento.CANCELADO).canceladoPor("admin").canceladoEm(AGORA)
                .motivoCancelamento("m").observacao("obs").build();
        assertThat(a.getId()).isEqualTo(9L);
        assertThat(a.ativo()).isFalse();
        assertThat(a.getObservacao()).isEqualTo("obs");
    }

    static Agendamento.Builder base() {
        return Agendamento.builder().data(AMANHA).faixaHorariaId(1L).idPedido(1L).fornecedor("F")
                .emailFornecedor("a@b.com").tipoCaminhaoId(1L).tipoPaletizacaoId(1L).quantidadePaletes(1)
                .paletesEquivalentes(1);
    }
}
