package br.com.redekrill.agendamento.domain.model;

import java.time.LocalDate;
import java.util.List;

public record OcupacaoDia(LocalDate data, int capacidade, int paletesOcupados, int quantidadeAgendamentos,
                          List<OcupacaoFaixa> faixas) {

    public double percentual() {
        return Percentual.de(paletesOcupados, capacidade);
    }

    public int excedente() {
        return Math.max(0, paletesOcupados - capacidade);
    }
}
