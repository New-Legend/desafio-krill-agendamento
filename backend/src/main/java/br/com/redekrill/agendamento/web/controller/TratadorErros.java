package br.com.redekrill.agendamento.web.controller;

import br.com.redekrill.agendamento.domain.exception.RecursoNaoEncontradoException;
import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.web.dto.Respostas.Erro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class TratadorErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorErros.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Erro> validacao(MethodArgumentNotValidException e) {
        List<String> det = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return erro(HttpStatus.BAD_REQUEST, "Dados inválidos", det);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<Erro> regra(RegraNegocioException e) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), List.of());
    }

    @ExceptionHandler({RecursoNaoEncontradoException.class, NoResourceFoundException.class})
    ResponseEntity<Erro> naoEncontrado(Exception e) {
        return erro(HttpStatus.NOT_FOUND, e instanceof RecursoNaoEncontradoException ? e.getMessage() : "Recurso não encontrado", List.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<Erro> requisicaoInvalida(Exception e) {
        return erro(HttpStatus.BAD_REQUEST, "Requisição inválida", List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Erro> credenciais(BadCredentialsException e) {
        return erro(HttpStatus.UNAUTHORIZED, e.getMessage(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Erro> acessoNegado(AccessDeniedException e) {
        return erro(HttpStatus.FORBIDDEN, "Acesso negado para o seu perfil", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Erro> integridade(DataIntegrityViolationException e) {
        return erro(HttpStatus.CONFLICT, "Registro conflita com dados existentes", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Erro> geral(Exception e) {
        log.error("Erro inesperado", e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", List.of());
    }

    private static ResponseEntity<Erro> erro(HttpStatus s, String msg, List<String> det) {
        return ResponseEntity.status(s).body(new Erro(s.value(), msg, det, OffsetDateTime.now()));
    }
}
