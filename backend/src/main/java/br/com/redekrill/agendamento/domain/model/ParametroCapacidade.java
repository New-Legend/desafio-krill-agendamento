package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

/** Parâmetros globais de capacidade do recebimento. */
public record ParametroCapacidade(int capacidadeDiariaPaletes, int quantidadeFaixas, int duracaoFaixaMinutos) {

    public static final ParametroCapacidade PADRAO = new ParametroCapacidade(480, 4, 120);

    public ParametroCapacidade {
        if (capacidadeDiariaPaletes < 1) {
            throw new RegraNegocioException("Capacidade diária deve ser maior que zero");
        }
        if (quantidadeFaixas < 1) {
            throw new RegraNegocioException("Quantidade de faixas deve ser maior que zero");
        }
        if (duracaoFaixaMinutos < 15) {
            throw new RegraNegocioException("Duração da faixa deve ter pelo menos 15 minutos");
        }
    }
}
