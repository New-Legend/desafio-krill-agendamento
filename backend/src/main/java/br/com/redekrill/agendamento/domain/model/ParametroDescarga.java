package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.math.BigDecimal;

/** Parâmetros de descarga por tipo de caminhão (porte de /recebimento-parametros-descarga). */
public record ParametroDescarga(Long tipoCaminhaoId, int minutosPrevistos, int toleranciaEsperaMinutos,
                                BigDecimal custoPorPalete) {
    public ParametroDescarga {
        if (tipoCaminhaoId == null) throw new RegraNegocioException("Tipo de caminhão é obrigatório");
        if (minutosPrevistos < 1 || minutosPrevistos > 1440) throw new RegraNegocioException("Tempo previsto deve ficar entre 1 e 1440 minutos");
        if (toleranciaEsperaMinutos < 0 || toleranciaEsperaMinutos > 1440) throw new RegraNegocioException("Tolerância de espera deve ficar entre 0 e 1440 minutos");
        if (custoPorPalete == null || custoPorPalete.signum() < 0) throw new RegraNegocioException("Custo por palete inválido");
    }
}
