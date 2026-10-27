package br.com.redekrill.agendamento.domain.service;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.ParametroCapacidade;

import java.util.List;
import java.util.Objects;

/** Garante que as faixas cadastradas respeitam os parâmetros (duração e quantidade) e não se sobrepõem. */
public final class ValidadorFaixas {

    public void validar(FaixaHoraria candidata, List<FaixaHoraria> existentes, ParametroCapacidade parametro) {
        if (candidata.duracaoMinutos() != parametro.duracaoFaixaMinutos()) {
            throw new RegraNegocioException("A faixa deve ter duração de " + parametro.duracaoFaixaMinutos() + " minutos");
        }
        List<FaixaHoraria> outrasAtivas = existentes.stream()
                .filter(FaixaHoraria::ativo)
                .filter(f -> !Objects.equals(f.id(), candidata.id()))
                .toList();
        if (candidata.ativo()) {
            outrasAtivas.stream().filter(candidata::sobrepoe).findFirst().ifPresent(f -> {
                throw new RegraNegocioException("A faixa sobrepõe a faixa " + f.descricao());
            });
            if (outrasAtivas.size() + 1 > parametro.quantidadeFaixas()) {
                throw new RegraNegocioException("Limite de " + parametro.quantidadeFaixas() + " faixas ativas atingido");
            }
        }
    }
}
