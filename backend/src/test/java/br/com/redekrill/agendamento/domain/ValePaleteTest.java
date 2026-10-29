package br.com.redekrill.agendamento.domain;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import br.com.redekrill.agendamento.domain.model.ValePalete.Assinante;
import br.com.redekrill.agendamento.domain.model.ValePalete.Quantidades;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static br.com.redekrill.agendamento.domain.AgendamentoTest.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValePaleteTest {

    static ValePalete vale(Quantidades q) {
        return new ValePalete(1L, "VP-1", null, LocalDate.of(2026, 11, 2), " Forn ", null, " ", q, null, null, " ", null, null, "admin", AGORA);
    }

    @Test
    void validacoes() {
        Quantidades q = br.com.redekrill.agendamento.TestePaletes.q(1, 0, 0, 0);
        assertThatThrownBy(() -> br.com.redekrill.agendamento.TestePaletes.q(-1, 0, 0, 0)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new ValePalete(1L, " ", null, LocalDate.now(), "F", null, null, q, null, null, null, null, null, "a", AGORA)).hasMessageContaining("Código");
        assertThatThrownBy(() -> new ValePalete(1L, "VP", null, null, "F", null, null, q, null, null, null, null, null, "a", AGORA)).hasMessageContaining("Data");
        assertThatThrownBy(() -> new ValePalete(1L, "VP", null, LocalDate.now(), null, null, null, q, null, null, null, null, null, "a", AGORA)).hasMessageContaining("Fornecedor");
        assertThatThrownBy(() -> new ValePalete(1L, "VP", null, LocalDate.now(), "x".repeat(81), null, null, q, null, null, null, null, null, "a", AGORA)).hasMessageContaining("80");
        assertThatThrownBy(() -> vale(Quantidades.ZERO)).hasMessageContaining("ao menos");
        assertThatThrownBy(() -> vale(null)).hasMessageContaining("ao menos");
        assertThatThrownBy(() -> new ValePalete(1L, "VP", null, LocalDate.now(), "F", null, null, q, br.com.redekrill.agendamento.TestePaletes.q(2, 0, 0, 0), null, null, null, null, "a", AGORA))
                .hasMessageContaining("Devolução");
        ValePalete v = vale(q);
        assertThat(v.getFornecedor()).isEqualTo("Forn");
        assertThat(v.getMotorista()).isNull();
        assertThat(v.getObservacao()).isNull();
        assertThat(ValePalete.gerarCodigo(LocalDate.of(2026, 11, 4), 12345)).isEqualTo("VP-20261104-2345");
    }

    @Test
    void assinaturasEDevolucao() {
        ValePalete v = vale(br.com.redekrill.agendamento.TestePaletes.q(4, 3, 2, 1));
        assertThat(v.saldo().total()).isEqualTo(10);
        assertThatThrownBy(() -> v.assinar(Assinante.MOTORISTA, " ", "data:image/png;base64,A", AGORA)).hasMessageContaining("nome");
        assertThatThrownBy(() -> v.assinar(Assinante.MOTORISTA, "J", "http://x", AGORA)).hasMessageContaining("inválida");
        assertThatThrownBy(() -> v.assinar(Assinante.MOTORISTA, "J", null, AGORA)).hasMessageContaining("inválida");
        assertThatThrownBy(() -> v.assinar(Assinante.MOTORISTA, "J", "data:image/png;base64," + "A".repeat(300_000), AGORA)).hasMessageContaining("grande");
        v.assinar(Assinante.MOTORISTA, "João", "data:image/png;base64,A", AGORA);
        assertThat(v.getStatus()).isEqualTo(ValePalete.Status.PENDENTE);
        v.assinar(Assinante.CONFERENTE, "Carlos", "data:image/png;base64,A", AGORA);
        assertThat(v.getStatus()).isEqualTo(ValePalete.Status.ASSINADO);
        assertThat(v.getAssinaturaConferente().nome()).isEqualTo("Carlos");
        v.removerAssinatura(Assinante.MOTORISTA);
        assertThat(v.getAssinaturaMotorista()).isNull();
        assertThatThrownBy(() -> v.registrarDevolucao(Quantidades.ZERO)).hasMessageContaining("Informe");
        assertThatThrownBy(() -> v.registrarDevolucao(br.com.redekrill.agendamento.TestePaletes.q(5, 0, 0, 0))).hasMessageContaining("saldo");
        v.registrarDevolucao(br.com.redekrill.agendamento.TestePaletes.q(4, 3, 2, 1));
        assertThat(v.getStatus()).isEqualTo(ValePalete.Status.QUITADO);
        assertThatThrownBy(v::cancelar).hasMessageContaining("quitado");
        assertThatThrownBy(() -> v.removerAssinatura(Assinante.CONFERENTE)).hasMessageContaining("quitado");
        ValePalete c = vale(br.com.redekrill.agendamento.TestePaletes.q(1, 0, 0, 0));
        c.cancelar();
        assertThatThrownBy(() -> c.registrarDevolucao(br.com.redekrill.agendamento.TestePaletes.q(1, 0, 0, 0))).hasMessageContaining("cancelado");
        assertThat(c.getCodigo()).isEqualTo("VP-1");
        assertThat(c.getId()).isEqualTo(1L);
        assertThat(c.getAgendamentoId()).isNull();
        assertThat(c.getCriadoPor()).isEqualTo("admin");
        assertThat(c.getCriadoEm()).isEqualTo(AGORA);
        assertThat(c.getData()).isNotNull();
        assertThat(c.getPlaca()).isNull();
    }

    @Test
    void fluxoAgendamento() {
        Agendamento a = AgendamentoTest.novo(AgendamentoTest.PALETIZADO, 10);
        assertThatThrownBy(() -> a.iniciarRecebimento("c", AGORA)).hasMessageContaining("Agendado");
        a.confirmar();
        assertThatThrownBy(a::confirmar).hasMessageContaining("Confirmado");
        a.atualizarPeloFornecedor(" ", null);
        assertThat(a.getPlaca()).isNull();
        assertThatThrownBy(() -> a.atualizarPeloFornecedor("123", null)).hasMessageContaining("Placa inválida");
        assertThatThrownBy(() -> a.registrarChegada(null, null, AGORA)).hasMessageContaining("Informe a placa");
        assertThatThrownBy(() -> a.registrarChegada("ABC1234", "x".repeat(81), AGORA)).hasMessageContaining("Motorista");
        a.registrarChegada("abc1234", " Zé ", AGORA);
        assertThat(a.getMotorista()).isEqualTo("Zé");
        assertThat(a.getChegadaEm()).isEqualTo(AGORA);
        assertThatThrownBy(() -> a.atualizarPeloFornecedor("ABC1234", null)).hasMessageContaining("Chegou");
        assertThatThrownBy(() -> a.iniciarRecebimento(" ", AGORA)).hasMessageContaining("conferente");
        a.iniciarRecebimento("Carlos", AGORA);
        assertThat(a.getConferente()).isEqualTo("Carlos");
        assertThat(a.getInicioRecebimentoEm()).isEqualTo(AGORA);
        assertThatThrownBy(() -> a.concluirRecebimento(null, null, AGORA)).hasMessageContaining("paletes");
        assertThatThrownBy(() -> a.concluirRecebimento(-1, null, AGORA)).hasMessageContaining("paletes");
        a.concluirRecebimento(10, "2 caixas avariadas", AGORA);
        assertThat(a.getStatus()).isEqualTo(StatusAgendamento.RECEBIDO);
        assertThat(a.divergente()).isTrue();
        assertThat(a.getFimRecebimentoEm()).isEqualTo(AGORA);
        assertThat(a.getPaletesRecebidos()).isEqualTo(10);
        assertThat(a.ativo()).isTrue();
        assertThat(StatusAgendamento.NAO_COMPARECEU.ocupaCapacidade()).isFalse();

        Agendamento b = AgendamentoTest.base().data(AGORA.toLocalDate()).build();
        b.marcarNaoCompareceu("admin", null, AGORA);
        assertThat(b.getMotivoCancelamento()).isEqualTo("Fornecedor não compareceu");
        assertThat(b.ativo()).isFalse();
        Agendamento c = AgendamentoTest.base().data(AGORA.toLocalDate()).build();
        c.marcarNaoCompareceu("admin", " atrasou ", AGORA);
        assertThat(c.getMotivoCancelamento()).isEqualTo("atrasou");
        c.definirTokenFornecedor("h");
        assertThat(c.getTokenFornecedorHash()).isEqualTo("h");
    }
}
