package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Movimento de paletes entre o CD e as lojas (porte de /paletes/romaneios, /retorno, /saldo-inicial).
 * ENVIO e SALDO_INICIAL aumentam o saldo da loja com o CD; RETORNO diminui.
 */
public record MovimentoPalete(Long id, LocalDate data, Tipo tipo, Long cdId, String loja, String romaneio,
                              ValePalete.Quantidades quantidades, String observacao, String criadoPor,
                              OffsetDateTime criadoEm) {

    public enum Tipo {
        ENVIO(1), RETORNO(-1), SALDO_INICIAL(1);
        private final int sinal;
        Tipo(int sinal) { this.sinal = sinal; }
        public int sinal() { return sinal; }
    }

    public MovimentoPalete {
        if (data == null) throw new RegraNegocioException("Data é obrigatória");
        if (tipo == null) throw new RegraNegocioException("Tipo do movimento é obrigatório");
        if (cdId == null) throw new RegraNegocioException("CD é obrigatório");
        loja = Textos.obrigatorio(loja, "Loja", 80);
        observacao = Textos.opcional(observacao, "Observação", 255);
        if (quantidades == null || quantidades.total() == 0) throw new RegraNegocioException("Informe ao menos um palete");
    }

    public int pbr() { return tipo.sinal() * quantidades.pbr(); }
    public int chep() { return tipo.sinal() * quantidades.chep(); }
    public int chapatex() { return tipo.sinal() * quantidades.chapatex(); }
    public int descartaveis() { return tipo.sinal() * quantidades.descartaveis(); }
    public int total() { return tipo.sinal() * quantidades.total(); }
}
