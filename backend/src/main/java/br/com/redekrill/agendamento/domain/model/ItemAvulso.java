package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Produto avulso lançado na conferência do recebimento, fora da NF-e importada. */
public record ItemAvulso(Long id, Long agendamentoId, Long produtoId, String codigo, String descricao, BigDecimal quantidade,
                         String unidade, String observacao, String criadoPor, OffsetDateTime criadoEm) {
    public ItemAvulso {
        if (agendamentoId == null) throw new RegraNegocioException("Agendamento é obrigatório");
        codigo = Textos.obrigatorio(codigo, "Código do produto", 60);
        descricao = Textos.obrigatorio(descricao, "Descrição", 120);
        if (quantidade == null || quantidade.signum() <= 0) throw new RegraNegocioException("Quantidade deve ser maior que zero");
        unidade = Textos.opcional(unidade, "Unidade", 6);
        observacao = Textos.opcional(observacao, "Observação", 255);
    }
}
