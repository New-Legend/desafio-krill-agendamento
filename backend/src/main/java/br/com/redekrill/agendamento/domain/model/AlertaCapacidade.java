package br.com.redekrill.agendamento.domain.model;

/** Aviso (não bloqueante) emitido quando a capacidade do dia ou da faixa é excedida. */
public record AlertaCapacidade(Tipo tipo, int capacidade, int ocupacaoResultante, int excedente, String mensagem) {
    public enum Tipo { DIA, FAIXA }
}
