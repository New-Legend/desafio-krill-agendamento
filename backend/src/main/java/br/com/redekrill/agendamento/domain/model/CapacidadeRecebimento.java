package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Capacidade diária de recebimento de um CD ou de uma loja (entrega direta), em paletes e/ou caminhões.
 * diaSemana nulo vale para todos os dias; 1 = segunda ... 7 = domingo sobrepõe a regra geral naquele dia.
 */
public record CapacidadeRecebimento(Long id, Long cdId, String loja, Integer diaSemana, Integer paletes, Integer caminhoes) {
    public CapacidadeRecebimento {
        loja = Textos.opcional(loja, "Loja", 80);
        if ((cdId == null) == (loja == null)) throw new RegraNegocioException("Informe o CD ou a loja (apenas um dos dois)");
        if (diaSemana != null && (diaSemana < 1 || diaSemana > 7)) throw new RegraNegocioException("Dia da semana deve ser de 1 (segunda) a 7 (domingo)");
        if (paletes == null && caminhoes == null) throw new RegraNegocioException("Informe a capacidade em paletes e/ou caminhões");
        if (paletes != null && paletes < 1) throw new RegraNegocioException("Capacidade em paletes deve ser maior que zero");
        if (caminhoes != null && caminhoes < 1) throw new RegraNegocioException("Capacidade em caminhões deve ser maior que zero");
    }

    public boolean valePara(LocalDate data) {
        return diaSemana == null || diaSemana == data.getDayOfWeek().getValue();
    }

    /** Regra do dia da semana tem prioridade sobre a regra geral. */
    public static Optional<CapacidadeRecebimento> resolver(List<CapacidadeRecebimento> regras, LocalDate data) {
        return regras.stream().filter(r -> r.valePara(data))
                .min(Comparator.comparing((CapacidadeRecebimento r) -> r.diaSemana() == null));
    }
}
