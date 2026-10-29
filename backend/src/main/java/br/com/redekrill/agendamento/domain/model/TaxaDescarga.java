package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.math.BigDecimal;

/** Taxa de descarga cobrada por palete conforme o modal (porte de recebimento-cobranca.ts do Gestão CD). */
public record TaxaDescarga(Long id, String modal, String nome, BigDecimal valorPorPalete, boolean ativo) {
    public TaxaDescarga {
        modal = CalculadoraTaxaDescarga.codigo(modal);
        if (!modal.matches("^[A-Z0-9_]{2,20}$")) throw new RegraNegocioException("Modal deve ter de 2 a 20 letras, números ou _");
        nome = Textos.obrigatorio(nome, "Nome do modal", 40);
        if (valorPorPalete == null || valorPorPalete.signum() < 0) throw new RegraNegocioException("Taxa por palete inválida");
    }
}
