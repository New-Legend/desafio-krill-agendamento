package br.com.redekrill.agendamento.domain.model;

/** Eventos do log de acessos (mesmos rótulos do Gestão CD). */
public enum EventoAcesso {
    ENTRADA("Entrou no sistema"),
    SAIDA("Saiu do sistema"),
    SENHA_RECUSADA("Senha recusada"),
    SENHA_TROCADA("Trocou a senha");

    private final String rotulo;

    EventoAcesso(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
