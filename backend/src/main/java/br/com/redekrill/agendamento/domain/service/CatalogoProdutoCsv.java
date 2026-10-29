package br.com.redekrill.agendamento.domain.service;

import br.com.redekrill.agendamento.domain.model.Produto;

import java.util.List;

/** Exporta o catálogo de produtos em CSV (separador ;, com BOM para abrir direto no Excel). */
public final class CatalogoProdutoCsv {
    private CatalogoProdutoCsv() {
    }

    public static String gerar(List<Produto> produtos) {
        StringBuilder sb = new StringBuilder("\uFEFFcodigo;ean;descricao;unidade;peso_bruto_kg;itens_por_palete;situacao\n");
        for (Produto p : produtos) {
            sb.append(campo(p.codigo())).append(';').append(campo(p.ean())).append(';').append(campo(p.descricao())).append(';')
                    .append(campo(p.unidade())).append(';')
                    .append(p.pesoBrutoKg() == null ? "" : p.pesoBrutoKg().toPlainString().replace('.', ',')).append(';')
                    .append(p.itensPorPalete() == null ? "" : p.itensPorPalete()).append(';')
                    .append(p.ativo() ? "ativo" : "inativo").append('\n');
        }
        return sb.toString();
    }

    /** Escapa aspas/separador e neutraliza injeção de fórmula (=, +, -, @). */
    static String campo(String v) {
        if (v == null) return "";
        String s = v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}
