package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

/**
 * Tipo de paletização. O fator multiplica a quantidade de paletes informada
 * para obter a ocupação real (Não paletizado = fator 2).
 */
public record TipoPaletizacao(Long id, String nome, int fatorMultiplicador, boolean ativo) {

    public TipoPaletizacao {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Nome do tipo de paletização é obrigatório");
        }
        if (fatorMultiplicador < 1) {
            throw new RegraNegocioException("Fator multiplicador deve ser maior ou igual a 1");
        }
        nome = nome.trim();
    }

    public int paletesEquivalentes(int quantidadePaletes) {
        return quantidadePaletes * fatorMultiplicador;
    }
}
