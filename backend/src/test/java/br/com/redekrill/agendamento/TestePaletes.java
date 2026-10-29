package br.com.redekrill.agendamento;

import br.com.redekrill.agendamento.domain.model.ValePalete;

import java.util.LinkedHashMap;
import java.util.Map;

/** Atalho dos testes: quantidades nos quatro tipos semeados (PBR, CHEP, CHAPATEX, DESCARTAVEL). */
public final class TestePaletes {
    private TestePaletes() {
    }

    public static ValePalete.Quantidades q(int pbr, int chep, int chapatex, int descartaveis) {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("PBR", pbr);
        m.put("CHEP", chep);
        m.put("CHAPATEX", chapatex);
        m.put("DESCARTAVEL", descartaveis);
        return new ValePalete.Quantidades(m);
    }
}
