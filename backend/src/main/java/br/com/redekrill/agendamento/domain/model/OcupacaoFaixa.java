package br.com.redekrill.agendamento.domain.model;

public record OcupacaoFaixa(FaixaHoraria faixa, int paletesOcupados) {

    public int capacidade() {
        return faixa.capacidadePaletes();
    }

    public double percentual() {
        return Percentual.de(paletesOcupados, capacidade());
    }

    public int excedente() {
        return Math.max(0, paletesOcupados - capacidade());
    }
}
