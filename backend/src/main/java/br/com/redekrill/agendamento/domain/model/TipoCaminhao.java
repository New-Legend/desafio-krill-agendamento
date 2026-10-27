package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

public record TipoCaminhao(Long id, String nome, String descricao, Integer capacidadePaletes, boolean ativo) {

    public TipoCaminhao {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Nome do tipo de caminhão é obrigatório");
        }
        if (capacidadePaletes != null && capacidadePaletes < 1) {
            throw new RegraNegocioException("Capacidade do caminhão deve ser maior que zero");
        }
        nome = nome.trim();
    }
}
