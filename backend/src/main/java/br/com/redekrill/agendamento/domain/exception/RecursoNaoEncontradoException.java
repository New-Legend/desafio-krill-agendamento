package br.com.redekrill.agendamento.domain.exception;

public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String entidade, Object id) {
        super(entidade + " não encontrado(a): " + id);
    }
}
