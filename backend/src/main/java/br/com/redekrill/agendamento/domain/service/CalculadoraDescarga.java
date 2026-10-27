package br.com.redekrill.agendamento.domain.service;

import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.ParametroDescarga;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;

/** Tempos de espera e descarga a partir dos horários da portaria e da conferência. */
public final class CalculadoraDescarga {

    public enum Situacao { AGUARDANDO, EM_DESCARGA, NO_PRAZO, ACIMA_DO_PREVISTO }

    public record Indicador(Long agendamentoId, Long esperaMinutos, Long descargaMinutos, Integer previstoMinutos,
                            Long excessoMinutos, boolean esperaAcimaTolerancia, BigDecimal custo, Situacao situacao) {
    }

    public Indicador calcular(Agendamento a, ParametroDescarga p, OffsetDateTime agora) {
        OffsetDateTime chegada = a.getChegadaEm();
        OffsetDateTime inicio = a.getInicioRecebimentoEm();
        OffsetDateTime fim = a.getFimRecebimentoEm();
        Long espera = chegada == null ? null : minutos(chegada, inicio == null ? agora : inicio);
        Long descarga = inicio == null ? null : minutos(inicio, fim == null ? agora : fim);
        Integer previsto = p == null ? null : p.minutosPrevistos();
        Long excesso = descarga == null || previsto == null ? null : Math.max(0, descarga - previsto);
        boolean esperaAcima = espera != null && p != null && espera > p.toleranciaEsperaMinutos();
        Integer paletes = a.getPaletesRecebidos() != null ? a.getPaletesRecebidos() : null;
        BigDecimal custo = p == null || paletes == null ? null
                : p.custoPorPalete().multiply(BigDecimal.valueOf(paletes)).setScale(2, RoundingMode.HALF_UP);
        Situacao s;
        if (inicio == null) s = Situacao.AGUARDANDO;
        else if (fim == null) s = Situacao.EM_DESCARGA;
        else s = excesso != null && excesso > 0 ? Situacao.ACIMA_DO_PREVISTO : Situacao.NO_PRAZO;
        return new Indicador(a.getId(), espera, descarga, previsto, excesso, esperaAcima, custo, s);
    }

    private static long minutos(OffsetDateTime de, OffsetDateTime ate) {
        return Math.max(0, Duration.between(de, ate).toMinutes());
    }
}
