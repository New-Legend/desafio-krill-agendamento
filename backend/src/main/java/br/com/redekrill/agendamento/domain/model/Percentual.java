package br.com.redekrill.agendamento.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Percentual {
    private Percentual() {
    }

    static double de(int valor, int total) {
        if (total <= 0) return 0;
        return BigDecimal.valueOf(valor * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
