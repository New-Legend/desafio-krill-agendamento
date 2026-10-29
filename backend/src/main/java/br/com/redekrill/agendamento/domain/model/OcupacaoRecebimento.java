package br.com.redekrill.agendamento.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Ocupação do dia de um destino (CD ou loja) frente à capacidade diária cadastrada. Capacidade nula = sem limite. */
public record OcupacaoRecebimento(LocalDate data, String destino, Integer capacidadePaletes, int paletes,
                                  Integer capacidadeCaminhoes, int caminhoes) {

    public double percentualPaletes() {
        return capacidadePaletes == null ? 0 : Percentual.de(paletes, capacidadePaletes);
    }

    public double percentualCaminhoes() {
        return capacidadeCaminhoes == null ? 0 : Percentual.de(caminhoes, capacidadeCaminhoes);
    }

    /** O maior dos dois percentuais: é o que define se o dia está cheio. */
    public double percentual() {
        return Math.max(percentualPaletes(), percentualCaminhoes());
    }

    public boolean limitado() {
        return capacidadePaletes != null || capacidadeCaminhoes != null;
    }

    public boolean cheio() {
        return limitado() && percentual() >= 100;
    }

    /** Alertas (não bloqueantes) quando a ocupação passou da capacidade. */
    public List<String> alertas() {
        List<String> r = new ArrayList<>();
        if (capacidadePaletes != null && paletes > capacidadePaletes) {
            r.add("Capacidade diária de " + destino + " excedida: " + paletes + " de " + capacidadePaletes + " paletes");
        }
        if (capacidadeCaminhoes != null && caminhoes > capacidadeCaminhoes) {
            r.add("Limite diário de caminhões de " + destino + " excedido: " + caminhoes + " de " + capacidadeCaminhoes);
        }
        return r;
    }
}
