package br.com.redekrill.agendamento.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Cálculo da taxa de descarga (porte de calcularCobrancaDescarga do Gestão CD): o modal vem do que foi
 * informado no agendamento ou é deduzido do tipo de caminhão (granel/rebater → GRANEL, baú → BAU, demais → SIDER).
 */
public final class CalculadoraTaxaDescarga {

    public record Cobranca(String modal, String nomeModal, int paletes, BigDecimal taxaPorPalete, BigDecimal valor) {
    }

    private CalculadoraTaxaDescarga() {
    }

    public static String codigo(String texto) {
        String s = Normalizer.normalize(texto == null ? "" : texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
    }

    public static String deduzirModal(String tipoCaminhao) {
        String n = codigo(tipoCaminhao);
        if (n.contains("GRANEL") || n.contains("REBATER")) return "GRANEL";
        if (n.contains("BAU")) return "BAU";
        return "SIDER";
    }

    public static Optional<Cobranca> calcular(List<TaxaDescarga> taxas, String modalInformado, String tipoCaminhao, int paletes) {
        String modal = modalInformado == null || modalInformado.isBlank() ? deduzirModal(tipoCaminhao) : codigo(modalInformado);
        return taxas.stream().filter(t -> t.ativo() && t.modal().equals(modal)).findFirst()
                .map(t -> new Cobranca(t.modal(), t.nome(), Math.max(0, paletes), t.valorPorPalete(),
                        t.valorPorPalete().multiply(BigDecimal.valueOf(Math.max(0, paletes))).setScale(2, RoundingMode.HALF_UP)));
    }
}
