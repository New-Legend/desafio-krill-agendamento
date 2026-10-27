package br.com.redekrill.agendamento.domain.service;

import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.AlertaCapacidade;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.OcupacaoDia;
import br.com.redekrill.agendamento.domain.model.OcupacaoFaixa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Regras puras de ocupação. Nunca bloqueia: apenas gera alertas quando o limite
 * diário ou da faixa é ultrapassado.
 */
public final class CalculadoraOcupacao {

    public OcupacaoDia calcular(LocalDate data, int capacidadeDiaria, List<FaixaHoraria> faixas,
                                List<Agendamento> agendamentos) {
        List<Agendamento> ativos = agendamentos.stream()
                .filter(Agendamento::ativo)
                .filter(a -> data.equals(a.getData()))
                .toList();
        int totalDia = ativos.stream().mapToInt(Agendamento::getPaletesEquivalentes).sum();
        List<OcupacaoFaixa> porFaixa = faixas.stream()
                .sorted(Comparator.comparing(FaixaHoraria::horaInicio))
                .map(f -> new OcupacaoFaixa(f, ativos.stream()
                        .filter(a -> Objects.equals(a.getFaixaHorariaId(), f.id()))
                        .mapToInt(Agendamento::getPaletesEquivalentes).sum()))
                .toList();
        return new OcupacaoDia(data, capacidadeDiaria, totalDia, ativos.size(), porFaixa);
    }

    /** Avalia os alertas que um novo volume geraria sobre a ocupação atual. */
    public List<AlertaCapacidade> avaliar(OcupacaoDia atual, Long faixaId, int novosPaletesEquivalentes) {
        List<AlertaCapacidade> alertas = new ArrayList<>();
        int dia = atual.paletesOcupados() + novosPaletesEquivalentes;
        if (dia > atual.capacidade()) {
            int exc = dia - atual.capacidade();
            alertas.add(new AlertaCapacidade(AlertaCapacidade.Tipo.DIA, atual.capacidade(), dia, exc,
                    "Capacidade diária excedida em " + exc + " paletes (" + dia + "/" + atual.capacidade() + ")"));
        }
        atual.faixas().stream()
                .filter(of -> Objects.equals(of.faixa().id(), faixaId))
                .findFirst()
                .ifPresent(of -> {
                    int faixa = of.paletesOcupados() + novosPaletesEquivalentes;
                    if (faixa > of.capacidade()) {
                        int exc = faixa - of.capacidade();
                        alertas.add(new AlertaCapacidade(AlertaCapacidade.Tipo.FAIXA, of.capacidade(), faixa, exc,
                                "Capacidade da faixa " + of.faixa().descricao() + " excedida em " + exc
                                        + " paletes (" + faixa + "/" + of.capacidade() + ")"));
                    }
                });
        return alertas;
    }
}
