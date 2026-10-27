package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Produto recebido (porte de /recebimento/produtos/pesos): peso bruto por unidade e itens por palete. */
public record Produto(Long id, String codigo, String ean, String descricao, String unidade, BigDecimal pesoBrutoKg,
                      Integer itensPorPalete, boolean ativo) {

    public Produto {
        codigo = Textos.obrigatorio(codigo, "Código do produto", 60);
        ean = Textos.opcional(Textos.digitos(ean), "EAN", 14);
        descricao = Textos.obrigatorio(descricao, "Descrição", 120);
        unidade = Textos.opcional(unidade, "Unidade", 6);
        if (pesoBrutoKg != null && pesoBrutoKg.signum() < 0) throw new RegraNegocioException("Peso não pode ser negativo");
        if (itensPorPalete != null && itensPorPalete < 1) throw new RegraNegocioException("Itens por palete deve ser maior que zero");
    }

    /** Peso total para uma quantidade, ou nulo quando o peso do produto não foi cadastrado. */
    public BigDecimal pesoPara(BigDecimal quantidade) {
        return pesoBrutoKg == null || quantidade == null ? null
                : pesoBrutoKg.multiply(quantidade).setScale(3, RoundingMode.HALF_UP);
    }

    /** Paletes estimados (arredonda para cima), ou nulo sem itens por palete. */
    public Integer paletesPara(BigDecimal quantidade) {
        return itensPorPalete == null || quantidade == null ? null
                : quantidade.divide(BigDecimal.valueOf(itensPorPalete), 0, RoundingMode.CEILING).intValue();
    }

    public Produto comPeso(BigDecimal peso, Integer porPalete) {
        return new Produto(id, codigo, ean, descricao, unidade, peso, porPalete, ativo);
    }
}
