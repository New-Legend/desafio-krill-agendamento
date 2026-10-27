package br.com.redekrill.agendamento.domain.exception;

/** Violação de uma regra de negócio (ex.: cancelar agendamento já cancelado). */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
