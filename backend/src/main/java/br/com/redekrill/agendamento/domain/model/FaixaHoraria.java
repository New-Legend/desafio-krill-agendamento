package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.time.Duration;
import java.time.LocalTime;

/** Faixa horária de recebimento (padrão: 4 faixas de 2 horas, 120 paletes cada). */
public record FaixaHoraria(Long id, LocalTime horaInicio, LocalTime horaFim, int capacidadePaletes, boolean ativo) {

    public FaixaHoraria {
        if (horaInicio == null || horaFim == null) {
            throw new RegraNegocioException("Hora inicial e final são obrigatórias");
        }
        if (!horaFim.isAfter(horaInicio)) {
            throw new RegraNegocioException("Hora final deve ser posterior à hora inicial");
        }
        if (capacidadePaletes < 1) {
            throw new RegraNegocioException("Capacidade da faixa deve ser maior que zero");
        }
    }

    public String descricao() {
        return horaInicio + " - " + horaFim;
    }

    public long duracaoMinutos() {
        return Duration.between(horaInicio, horaFim).toMinutes();
    }

    public boolean sobrepoe(FaixaHoraria outra) {
        return horaInicio.isBefore(outra.horaFim) && outra.horaInicio.isBefore(horaFim);
    }
}
