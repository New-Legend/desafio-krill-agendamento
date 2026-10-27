package br.com.redekrill.agendamento.domain.model;

/** Ciclo de vida do agendamento (mesmo fluxo do Gestão CD: agenda → portaria → conferência). */
public enum StatusAgendamento {
    AGENDADO("Agendado"),
    CONFIRMADO("Confirmado"),
    CHEGOU("Chegou"),
    EM_RECEBIMENTO("Em recebimento"),
    RECEBIDO("Recebido"),
    CANCELADO("Cancelado"),
    NAO_COMPARECEU("Não compareceu");

    private final String rotulo;

    StatusAgendamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Conta na ocupação do dia/faixa. */
    public boolean ocupaCapacidade() {
        return this != CANCELADO && this != NAO_COMPARECEU;
    }

    /** Antes da chegada: pode cancelar, confirmar e o fornecedor pode editar pelo link. */
    public boolean antesDaChegada() {
        return this == AGENDADO || this == CONFIRMADO;
    }
}
