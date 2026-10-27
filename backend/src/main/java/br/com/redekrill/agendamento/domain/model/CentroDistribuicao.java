package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.util.List;

/**
 * Centro de distribuição (multi-CD). Capacidades nulas herdam o parâmetro geral (480/dia e a
 * capacidade de cada faixa), então o CD padrão se comporta exatamente como a regra do desafio.
 */
public record CentroDistribuicao(Long id, String codigo, String nome, String cidade, Integer capacidadeDiariaPaletes,
                                 Integer capacidadeFaixaPaletes, boolean ativo) {

    public CentroDistribuicao {
        codigo = Textos.obrigatorio(codigo, "Código do CD", 10);
        nome = Textos.obrigatorio(nome, "Nome do CD", 80);
        cidade = Textos.opcional(cidade, "Cidade", 80);
        if (capacidadeDiariaPaletes != null && capacidadeDiariaPaletes < 1) {
            throw new RegraNegocioException("Capacidade diária do CD deve ser maior que zero");
        }
        if (capacidadeFaixaPaletes != null && capacidadeFaixaPaletes < 1) {
            throw new RegraNegocioException("Capacidade por faixa do CD deve ser maior que zero");
        }
    }

    public int capacidadeDia(int padrao) {
        return capacidadeDiariaPaletes == null ? padrao : capacidadeDiariaPaletes;
    }

    /** Faixas com a capacidade própria do CD (quando definida). */
    public List<FaixaHoraria> ajustar(List<FaixaHoraria> faixas) {
        if (capacidadeFaixaPaletes == null) return faixas;
        return faixas.stream()
                .map(f -> new FaixaHoraria(f.id(), f.horaInicio(), f.horaFim(), capacidadeFaixaPaletes, f.ativo()))
                .toList();
    }
}
